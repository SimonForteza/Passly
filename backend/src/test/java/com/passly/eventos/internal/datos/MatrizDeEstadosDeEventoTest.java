package com.passly.eventos.internal.datos;

import com.passly.eventos.CupoInsuficienteException;
import com.passly.eventos.EdicionDeEventoInvalidaException;
import com.passly.eventos.EstadoEvento;
import com.passly.eventos.TransicionDeEstadoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.passly.eventos.EstadoEvento.BORRADOR;
import static com.passly.eventos.EstadoEvento.CANCELADO;
import static com.passly.eventos.EstadoEvento.PUBLICADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La matriz completa <b>estado x operacion</b> del patron State de {@link Evento} (PAS-35).
 *
 * <p>Unitario puro: sin contexto de Spring, sin base de datos. El {@link Evento} se arma con su
 * constructor y se lleva a cada estado con las <b>operaciones reales</b> ({@code publicar},
 * {@code cancelar}), no forzando el campo. Lo unico que se asigna por reflexion son los ids de los
 * tipos de entrada, que en produccion pone la base ({@code ReflectionTestUtils} es una utilidad de
 * spring-test, no levanta ningun contexto).
 *
 * <p>Es la misma matriz que documenta CLAUDE.md §9 (PAS-19), mas la columna nueva de cancelar.
 */
class MatrizDeEstadosDeEventoTest {

    private static final long ID_TIPO = 10L;

    enum Operacion {
        PUBLICAR(e -> e.publicar()),
        CANCELAR(e -> e.cancelar()),
        EDITAR_DATOS(e -> e.editarDatos("Otro", "otra", OffsetDateTime.now().plusMonths(2), "Otro lugar")),
        EDITAR_TIPO_ENTRADA(e -> e.editarTipoEntrada(ID_TIPO, "General", new BigDecimal("900.00"), 20)),
        AGREGAR_TIPO_ENTRADA(e -> e.sumarTipoEntrada("VIP", new BigDecimal("5000.00"), 10)),
        AMPLIAR_CUPO(e -> e.ampliarCupo(ID_TIPO, 5)),
        CAMBIAR_PORTADA(e -> e.cambiarPortada(OffsetDateTime.now())),
        QUITAR_PORTADA(e -> e.quitarPortada());

        final Consumer<Evento> aplicar;

        Operacion(Consumer<Evento> aplicar) {
            this.aplicar = aplicar;
        }
    }

    /** {@code null} = permitida; si no, la excepcion (409) que tiene que lanzar. */
    static Stream<Arguments> matriz() {
        Class<?> transicion = TransicionDeEstadoInvalidaException.class;
        Class<?> edicion = EdicionDeEventoInvalidaException.class;
        return Stream.of(
                Arguments.of(BORRADOR, Operacion.PUBLICAR, null),
                Arguments.of(BORRADOR, Operacion.CANCELAR, null),
                Arguments.of(BORRADOR, Operacion.EDITAR_DATOS, null),
                Arguments.of(BORRADOR, Operacion.EDITAR_TIPO_ENTRADA, null),
                Arguments.of(BORRADOR, Operacion.AGREGAR_TIPO_ENTRADA, null),
                Arguments.of(BORRADOR, Operacion.AMPLIAR_CUPO, null),
                Arguments.of(BORRADOR, Operacion.CAMBIAR_PORTADA, null),
                Arguments.of(BORRADOR, Operacion.QUITAR_PORTADA, null),

                Arguments.of(PUBLICADO, Operacion.PUBLICAR, transicion),
                Arguments.of(PUBLICADO, Operacion.CANCELAR, null),
                Arguments.of(PUBLICADO, Operacion.EDITAR_DATOS, edicion),
                Arguments.of(PUBLICADO, Operacion.EDITAR_TIPO_ENTRADA, edicion),
                Arguments.of(PUBLICADO, Operacion.AGREGAR_TIPO_ENTRADA, null),
                Arguments.of(PUBLICADO, Operacion.AMPLIAR_CUPO, null),
                Arguments.of(PUBLICADO, Operacion.CAMBIAR_PORTADA, null),
                Arguments.of(PUBLICADO, Operacion.QUITAR_PORTADA, null),

                Arguments.of(CANCELADO, Operacion.PUBLICAR, transicion),
                Arguments.of(CANCELADO, Operacion.CANCELAR, transicion),
                Arguments.of(CANCELADO, Operacion.EDITAR_DATOS, edicion),
                Arguments.of(CANCELADO, Operacion.EDITAR_TIPO_ENTRADA, edicion),
                Arguments.of(CANCELADO, Operacion.AGREGAR_TIPO_ENTRADA, edicion),
                Arguments.of(CANCELADO, Operacion.AMPLIAR_CUPO, edicion),
                Arguments.of(CANCELADO, Operacion.CAMBIAR_PORTADA, edicion),
                Arguments.of(CANCELADO, Operacion.QUITAR_PORTADA, edicion)
        );
    }

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("matriz")
    void cadaEstadoDecideQueOperacionesAdmite(EstadoEvento estado, Operacion operacion, Class<?> esperada) {
        Evento evento = eventoEn(estado);

        if (esperada == null) {
            assertThatCode(() -> operacion.aplicar.accept(evento)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> operacion.aplicar.accept(evento)).isInstanceOf(esperada);
            assertThat(evento.getEstado()).as("un rechazo no cambia el estado").isEqualTo(estado);
        }
    }

    @Test
    void publicarUnBorradorLoLlevaAPublicado() {
        Evento evento = eventoEn(BORRADOR);
        evento.publicar();
        assertThat(evento.getEstado()).isEqualTo(PUBLICADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoEvento.class, names = {"BORRADOR", "PUBLICADO"})
    void cancelarDesdeBorradorOPublicadoLoLlevaACancelado(EstadoEvento desde) {
        Evento evento = eventoEn(desde);
        evento.cancelar();
        assertThat(evento.getEstado()).isEqualTo(CANCELADO);
    }

    @Test
    void soloUnEventoPublicadoAdmiteVentas() {
        assertThat(eventoEn(BORRADOR).admiteVentas()).isFalse();
        assertThat(eventoEn(PUBLICADO).admiteVentas()).isTrue();
        assertThat(eventoEn(CANCELADO).admiteVentas()).isFalse();
    }

    @Test
    void publicarUnBorradorConFechaPasadaLanzaTransicionInvalida() {
        Evento evento = nuevoEvento(OffsetDateTime.now().minusDays(1), 5);
        assertThatThrownBy(evento::publicar).isInstanceOf(TransicionDeEstadoInvalidaException.class);
        assertThat(evento.getEstado()).isEqualTo(BORRADOR);
    }

    @Test
    void publicarUnBorradorSinCupoLanzaTransicionInvalida() {
        Evento evento = nuevoEvento(OffsetDateTime.now().plusMonths(1), 0);
        assertThatThrownBy(evento::publicar).isInstanceOf(TransicionDeEstadoInvalidaException.class);
        assertThat(evento.getEstado()).isEqualTo(BORRADOR);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoEvento.class, names = {"BORRADOR", "CANCELADO"})
    void descontarCupoSinAdmitirVentasLanzaCupoInsuficienteYNoDescuenta(EstadoEvento estado) {
        Evento evento = eventoEn(estado);
        assertThatThrownBy(() -> evento.descontarCupo(ID_TIPO, 1)).isInstanceOf(CupoInsuficienteException.class);
        assertThat(evento.getTiposEntrada().get(0).getCupoDisponible()).isEqualTo(5);
    }

    @Test
    void descontarCupoDeUnPublicadoDescuenta() {
        Evento evento = eventoEn(PUBLICADO);
        evento.descontarCupo(ID_TIPO, 2);
        assertThat(evento.getTiposEntrada().get(0).getCupoDisponible()).isEqualTo(3);
    }

    @Test
    void elEstadoSeResuelveDesdeElEnumPersistido() {
        for (EstadoEvento estado : EstadoEvento.values()) {
            assertThat(EstadoDeEvento.de(estado).valor()).isEqualTo(estado);
        }
    }

    /** Un evento futuro con un tipo de entrada de 5 lugares, llevado a {@code estado} por la via real. */
    private static Evento eventoEn(EstadoEvento estado) {
        Evento evento = nuevoEvento(OffsetDateTime.now().plusMonths(1), 5);
        switch (estado) {
            case BORRADOR -> { }
            case PUBLICADO -> evento.publicar();
            case CANCELADO -> evento.cancelar();
        }
        return evento;
    }

    private static Evento nuevoEvento(OffsetDateTime fecha, int cupo) {
        Evento evento = new Evento(1L, "Fiesta", "desc", fecha, "Lugar");
        TipoEntrada general = new TipoEntrada("General", new BigDecimal("1000.00"), cupo);
        ReflectionTestUtils.setField(general, "id", ID_TIPO);
        evento.agregarTipoEntrada(general);
        return evento;
    }
}
