package com.passly.ventas.internal.negocio;

import com.passly.eventos.EventoService;
import com.passly.pagos.PasarelaDePagoNoDisponibleException;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import com.passly.ventas.CarritoVacioException;
import com.passly.ventas.PagoRechazadoException;
import com.passly.ventas.dto.DatosDeTarjetaDTO;
import com.passly.ventas.internal.datos.Orden;
import com.passly.ventas.internal.datos.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unitario puro (sin Spring ni base) del Facade de la compra (PAS-38). Fija la regla central de
 * CLAUDE.md 2 y 4.11: el cobro va <b>antes</b> y <b>afuera</b> de la transaccion, y si la
 * confirmacion falla despues de cobrar, se compensa.
 *
 * <p>"No abrir la transaccion" se traduce aca en no tocar {@link ConfirmacionDeCompra}, que es el
 * unico bean {@code @Transactional} del flujo. El carrito es real (es un POJO con estado): asi el
 * test tambien verifica que no se vacia cuando la compra no se concreta.
 */
class VentaServiceImplTest {

    private static final Long COMPRADOR = 7L;
    private static final DatosDeTarjetaDTO TARJETA =
            new DatosDeTarjetaDTO("4242424242424242", "Carla", "12/30", "123");

    private final EventoService eventos = mock(EventoService.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final PasarelaDePago pasarela = mock(PasarelaDePago.class);
    private final ConfirmacionDeCompra confirmacion = mock(ConfirmacionDeCompra.class);
    private final OrdenRepository ordenes = mock(OrdenRepository.class);
    private final VentaMapper mapper = mock(VentaMapper.class);
    private final CarritoDeCompra carrito = new CarritoDeCompra();
    private final VentaServiceImpl servicio =
            new VentaServiceImpl(carrito, eventos, usuarios, pasarela, confirmacion, ordenes, mapper);

    private final ComprobanteDeCobroDTO comprobante =
            new ComprobanteDeCobroDTO("cobro-41", new BigDecimal("3000.00"), OffsetDateTime.now());

    @BeforeEach
    void carritoConDosEntradas() {
        carrito.agregarItem(COMPRADOR, 11L, 3L, "General", new BigDecimal("1500.00"), 2);
        when(usuarios.consultarUsuario(COMPRADOR))
                .thenReturn(new UsuarioDTO(COMPRADOR, "carla@passly.test", "Carla", Rol.COMPRADOR));
    }

    @Test
    void cobroAprobadoConfirmaLaOrdenYVaciaElCarrito() {
        Orden orden = mock(Orden.class);
        when(pasarela.cobrar(any(), anyString(), eq(TARJETA))).thenReturn(comprobante);
        when(confirmacion.confirmar(eq(COMPRADOR), any(), any(), eq("cobro-41"), any())).thenReturn(orden);

        servicio.confirmarCompra(COMPRADOR, TARJETA);

        verify(pasarela).cobrar(eq(new BigDecimal("3000.00")), anyString(), eq(TARJETA));
        verify(mapper).aDTO(eq(orden), any());
        verify(pasarela, never()).revertir(anyString());
        assertThat(carrito.verComo(COMPRADOR).items()).isEmpty();
    }

    @Test
    void pagoRechazadoNoConfirmaNiVaciaElCarrito() {
        when(pasarela.cobrar(any(), anyString(), any())).thenThrow(new PagoRechazadoException("fondos insuficientes"));

        assertThatThrownBy(() -> servicio.confirmarCompra(COMPRADOR, TARJETA))
                .isInstanceOf(PagoRechazadoException.class);

        verifyNoInteractions(confirmacion);
        assertThat(carrito.verComo(COMPRADOR).items()).hasSize(1);
    }

    @Test
    void pasarelaCaidaSePropagaSinAbrirLaTransaccion() {
        var caida = new PasarelaDePagoNoDisponibleException("ref", new IllegalStateException("timeout"));
        when(pasarela.cobrar(any(), anyString(), any())).thenThrow(caida);

        assertThatThrownBy(() -> servicio.confirmarCompra(COMPRADOR, TARJETA)).isSameAs(caida);

        verifyNoInteractions(confirmacion);
        verify(pasarela, never()).revertir(anyString());
    }

    @Test
    void siLaConfirmacionFallaDespuesDeCobrarSeRevierteElCobroYSeRelanzaLaFalla() {
        var falla = new IllegalStateException("cupo insuficiente");
        when(pasarela.cobrar(any(), anyString(), any())).thenReturn(comprobante);
        when(confirmacion.confirmar(any(), any(), any(), any(), any())).thenThrow(falla);

        assertThatThrownBy(() -> servicio.confirmarCompra(COMPRADOR, TARJETA)).isSameAs(falla);

        verify(pasarela).revertir("cobro-41");
        assertThat(carrito.verComo(COMPRADOR).items())
                .as("el carrito sobrevive para reintentar")
                .hasSize(1);
    }

    @Test
    void siTambienFallaLaReversionResponde503ConAmbasFallas() {
        var falla = new IllegalStateException("cupo insuficiente");
        var fallaReversion = new IllegalStateException("pasarela sin respuesta");
        when(pasarela.cobrar(any(), anyString(), any())).thenReturn(comprobante);
        when(confirmacion.confirmar(any(), any(), any(), any(), any())).thenThrow(falla);
        doThrow(fallaReversion).when(pasarela).revertir("cobro-41");

        assertThatThrownBy(() -> servicio.confirmarCompra(COMPRADOR, TARJETA))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getCause()).isSameAs(falla);
                    assertThat(falla.getSuppressed()).containsExactly(fallaReversion);
                });
    }

    @Test
    void carritoVacioNoLlegaACobrar() {
        var vacio = new CarritoDeCompra();
        var otro = new VentaServiceImpl(vacio, eventos, usuarios, pasarela, confirmacion, ordenes, mapper);

        assertThatThrownBy(() -> otro.confirmarCompra(COMPRADOR, TARJETA))
                .isInstanceOf(CarritoVacioException.class);

        verifyNoInteractions(pasarela, confirmacion);
    }

    @Test
    void consultarUnaOrdenAjenaEsNoEncontrada() {
        when(ordenes.findByIdAndCompradorId(99L, COMPRADOR)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> servicio.consultarOrden(99L, COMPRADOR))
                .isInstanceOf(com.passly.ventas.OrdenNoEncontradaException.class);
        verify(mapper, never()).aDTO(any(Orden.class), any());
    }

    @Test
    void listarOrdenesResuelveElCompradorUnaSolaVez() {
        when(ordenes.findByCompradorIdOrderByCreadaEnDesc(COMPRADOR))
                .thenReturn(List.of(mock(Orden.class), mock(Orden.class)));

        assertThat(servicio.listarOrdenesDeComprador(COMPRADOR)).hasSize(2);
        verify(usuarios).consultarUsuario(COMPRADOR);
    }
}
