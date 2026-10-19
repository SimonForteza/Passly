package com.passly.ventas.internal.negocio;

import com.passly.pagos.EstadoDePago;
import com.passly.pagos.PagoService;
import com.passly.pagos.PasarelaDePagoNoDisponibleException;
import com.passly.pagos.dto.ResultadoDeCobroDTO;
import com.passly.pagos.dto.SolicitudDeCobroDTO;
import com.passly.ventas.dto.DatosDeTarjetaDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class PasarelaDePagoRealTest {

    private final PagoService pagos = Mockito.mock(PagoService.class);
    private final PasarelaDePagoReal pasarela = new PasarelaDePagoReal(pagos);

    @Test
    void usaLaReferenciaYLaTarjetaSinCambiarElImporte() {
        var tarjeta = new DatosDeTarjetaDTO("4242424242424242", "Invitada", "12/30", "123");
        var autorizado = OffsetDateTime.now();
        when(pagos.cobrar(Mockito.any())).thenReturn(new ResultadoDeCobroDTO(
                41L, "ext-41", EstadoDePago.APROBADO, new BigDecimal("25.00"), "ARS", autorizado));

        var resultado = pasarela.cobrar(new BigDecimal("25.00"), "referencia-intento", tarjeta);

        var solicitud = ArgumentCaptor.forClass(SolicitudDeCobroDTO.class);
        Mockito.verify(pagos).cobrar(solicitud.capture());
        assertThat(solicitud.getValue().referencia()).isEqualTo("referencia-intento");
        assertThat(solicitud.getValue().monto()).isEqualByComparingTo("25.00");
        assertThat(solicitud.getValue().tarjeta().numero()).isEqualTo("4242424242424242");
        assertThat(resultado.id()).isEqualTo("41");
    }

    @Test
    void propagaUnaReversionFallidaParaQueLaCompraQuedeEnRevision() {
        var falla = new PasarelaDePagoNoDisponibleException("referencia-intento", new IllegalStateException("sin respuesta"));
        Mockito.doThrow(falla).when(pagos).revertir(41L);

        assertThatThrownBy(() -> pasarela.revertir("41")).isSameAs(falla);
    }
}
