package com.passly.eventos;

import com.passly.eventos.dto.AmpliarCupoRequest;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.DescontarCupoRequest;
import com.passly.eventos.dto.EditarEventoRequest;
import com.passly.eventos.dto.EditarTipoEntradaRequest;
import com.passly.eventos.dto.EventoDTO;
import com.passly.eventos.dto.TipoEntradaDTO;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code editarEvento}, {@code agregarTipoEntrada}, {@code editarTipoEntrada} y
 * {@code ampliarCupo} (PAS-19): la matriz de que se puede editar segun el estado del evento
 * (CLAUDE.md 4.11-19 — ver el informe) es el corazon de esta suite.
 *
 * <p>Mismo fixture que {@link DescuentoDeCupoTest}: organizador + productora + evento por los
 * contratos publicos de Usuarios y Productoras, con datos aleatorios (UUID), sin depender del
 * perfil {@code demo}.
 */
@SpringBootTest
class EdicionDeEventoTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoraService productoraService;

    @Autowired
    private EventoService eventoService;

    @Test
    void editarDatosEnBorradorLosCambia() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);

        EventoDTO editado = eventoService.editarEvento(
                fixture.evento.id(),
                new EditarEventoRequest("Nuevo nombre", "Nueva descripcion",
                        OffsetDateTime.now().plusMonths(2), "Nuevo lugar"),
                fixture.idOrganizador);

        assertThat(editado.nombre()).isEqualTo("Nuevo nombre");
        assertThat(editado.descripcion()).isEqualTo("Nueva descripcion");
        assertThat(editado.lugar()).isEqualTo("Nuevo lugar");
    }

    @Test
    void editarDatosDeUnEventoPublicadoLanzaEdicionInvalida() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        eventoService.publicarEvento(fixture.evento.id(), fixture.idOrganizador);

        assertThatThrownBy(() -> eventoService.editarEvento(
                fixture.evento.id(),
                new EditarEventoRequest("Otro nombre", "otra", OffsetDateTime.now().plusMonths(2), "otro lugar"),
                fixture.idOrganizador))
                .isInstanceOf(EdicionDeEventoInvalidaException.class);
    }

    @Test
    void agregarTipoDeEntradaAUnEventoPublicadoLoSuma() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        eventoService.publicarEvento(fixture.evento.id(), fixture.idOrganizador);

        EventoDTO actualizado = eventoService.agregarTipoEntrada(
                fixture.evento.id(),
                new CrearTipoEntradaRequest("VIP", new BigDecimal("3000.00"), 10),
                fixture.idOrganizador);

        assertThat(actualizado.tiposEntrada()).hasSize(2);
        TipoEntradaDTO vip = actualizado.tiposEntrada().stream()
                .filter(t -> t.nombre().equals("VIP"))
                .findFirst().orElseThrow();
        // El id tiene que venir poblado en la misma respuesta: sin el flush explicito en
        // EventoServiceImpl.agregarTipoEntrada, esto viajaba en null (bug real, encontrado
        // probando el endpoint en vivo, no con este test).
        assertThat(vip.id()).isNotNull();
        assertThat(vip.cupoTotal()).isEqualTo(10);
        assertThat(vip.cupoDisponible()).isEqualTo(10);
    }

    @Test
    void agregarTipoDeEntradaConNombreRepetidoLanzaEdicionInvalida() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);

        assertThatThrownBy(() -> eventoService.agregarTipoEntrada(
                fixture.evento.id(),
                new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), 10),
                fixture.idOrganizador))
                .isInstanceOf(EdicionDeEventoInvalidaException.class);
    }

    @Test
    void ampliarCupoEnUnEventoPublicadoConVentasPreviasSumaAlTotalYAlDisponible() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        eventoService.publicarEvento(fixture.evento.id(), fixture.idOrganizador);
        Long idTipoEntrada = fixture.evento.tiposEntrada().get(0).id();
        eventoService.descontarCupo(List.of(new DescontarCupoRequest(idTipoEntrada, 3)));

        EventoDTO actualizado = eventoService.ampliarCupo(
                fixture.evento.id(), idTipoEntrada, new AmpliarCupoRequest(4), fixture.idOrganizador);

        TipoEntradaDTO tipo = actualizado.tiposEntrada().get(0);
        assertThat(tipo.cupoTotal()).isEqualTo(9); // 5 + 4
        assertThat(tipo.cupoDisponible()).isEqualTo(6); // (5-3) + 4
    }

    @Test
    void editarTipoDeEntradaEnBorradorBajandoElCupoPorDebajoDeLoVendidoLanzaEdicionInvalidaYNoDejaCambios() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        Long idTipoEntrada = fixture.evento.tiposEntrada().get(0).id();
        eventoService.descontarCupo(List.of(new DescontarCupoRequest(idTipoEntrada, 3)));

        assertThatThrownBy(() -> eventoService.editarTipoEntrada(
                fixture.evento.id(), idTipoEntrada,
                new EditarTipoEntradaRequest("General", new BigDecimal("1000.00"), 2),
                fixture.idOrganizador))
                .isInstanceOf(EdicionDeEventoInvalidaException.class);

        assertThat(eventoService.consultarDisponibilidad(idTipoEntrada).cupoDisponible()).isEqualTo(2);
    }

    @Test
    void editarTipoDeEntradaEnBorradorSubiendoElCupoPorEncimaDeLoVendidoRecalculaElDisponible() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        Long idTipoEntrada = fixture.evento.tiposEntrada().get(0).id();
        eventoService.descontarCupo(List.of(new DescontarCupoRequest(idTipoEntrada, 3)));

        EventoDTO actualizado = eventoService.editarTipoEntrada(
                fixture.evento.id(), idTipoEntrada,
                new EditarTipoEntradaRequest("General VIP", new BigDecimal("1500.00"), 10),
                fixture.idOrganizador);

        TipoEntradaDTO tipo = actualizado.tiposEntrada().get(0);
        assertThat(tipo.nombre()).isEqualTo("General VIP");
        assertThat(tipo.precio()).isEqualByComparingTo("1500.00");
        assertThat(tipo.cupoTotal()).isEqualTo(10);
        assertThat(tipo.cupoDisponible()).isEqualTo(7); // 10 - 3 vendidas
    }

    @Test
    void editarTipoDeEntradaDeUnEventoPublicadoLanzaEdicionInvalida() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        eventoService.publicarEvento(fixture.evento.id(), fixture.idOrganizador);
        Long idTipoEntrada = fixture.evento.tiposEntrada().get(0).id();

        assertThatThrownBy(() -> eventoService.editarTipoEntrada(
                fixture.evento.id(), idTipoEntrada,
                new EditarTipoEntradaRequest("General", new BigDecimal("1000.00"), 20),
                fixture.idOrganizador))
                .isInstanceOf(EdicionDeEventoInvalidaException.class);
    }

    @Test
    void editarEventoDeOtroOrganizadorSinMembresiaLanzaProductoraNoGestionable() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);

        UsuarioDTO otroOrganizador = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "otro-organizador-" + UUID.randomUUID() + "@test.passly",
                "Otro Organizador",
                "passly1234",
                Rol.ORGANIZADOR));

        assertThatThrownBy(() -> eventoService.editarEvento(
                fixture.evento.id(),
                new EditarEventoRequest("Nombre ajeno", "desc", OffsetDateTime.now().plusMonths(2), "lugar"),
                otroOrganizador.id()))
                .isInstanceOf(ProductoraNoGestionableException.class);
    }

    @Test
    void editarTipoDeEntradaDeOtroEventoLanzaTipoEntradaNoEncontrado() {
        Fixture fixture = crearEventoConUnTipoDeEntrada(5);
        Fixture otroFixture = crearEventoConUnTipoDeEntrada(5);
        Long idTipoDeOtroEvento = otroFixture.evento.tiposEntrada().get(0).id();

        assertThatThrownBy(() -> eventoService.editarTipoEntrada(
                fixture.evento.id(), idTipoDeOtroEvento,
                new EditarTipoEntradaRequest("General", new BigDecimal("1000.00"), 10),
                fixture.idOrganizador))
                .isInstanceOf(TipoEntradaNoEncontradoException.class);
    }

    private record Fixture(EventoDTO evento, Long idOrganizador) {
    }

    private Fixture crearEventoConUnTipoDeEntrada(int cupoTotal) {
        String sufijo = UUID.randomUUID().toString();

        UsuarioDTO organizador = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador-" + sufijo + "@test.passly",
                "Organizador de Prueba",
                "passly1234",
                Rol.ORGANIZADOR));

        Long idProductora = productoraService.crearProductora(
                new CrearProductoraRequest("Productora " + sufijo, null, null, null),
                organizador.id()
        ).id();

        EventoDTO evento = eventoService.crearEvento(new CrearEventoRequest(
                idProductora,
                "Evento de Prueba " + sufijo,
                "Fixture de EdicionDeEventoTest",
                OffsetDateTime.now().plusMonths(1),
                "Lugar de Prueba",
                List.of(new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), cupoTotal))
        ), organizador.id());

        return new Fixture(evento, organizador.id());
    }
}
