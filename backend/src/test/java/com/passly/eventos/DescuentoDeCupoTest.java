package com.passly.eventos;

import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.DescontarCupoRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EventoService#descontarCupo(List)} es el contrato nuevo que {@code ServicioDeVentas}
 * (PAS-8) usa para confirmar una compra. Esta suite prueba, sin necesidad de que Ventas exista
 * todavia, las dos garantias en las que ese diseño se apoya:
 *
 * <ol>
 *   <li>una linea insuficiente no deja cambios a medias — ni en ella ni en las que la
 *       acompañaban en el mismo lote, porque todo el metodo es una sola transaccion;</li>
 *   <li>el {@code @Version} de {@code TipoEntrada} convierte una carrera real entre dos
 *       descuentos concurrentes sobre el mismo tipo de entrada en un
 *       {@code ObjectOptimisticLockingFailureException}, nunca en una sobreventa silenciosa.</li>
 * </ol>
 *
 * <p>El fixture (usuario ORGANIZADOR + productora + evento) se arma en cada test por los
 * contratos publicos de Usuarios y Productoras, con datos aleatorios (UUID), para no depender
 * del perfil {@code demo} ni de que la base este vacia.
 */
@SpringBootTest
class DescuentoDeCupoTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoraService productoraService;

    @Autowired
    private EventoService eventoService;

    @Test
    void unDescuentoValidoDecrementaElCupoDisponible() {
        EventoDTO evento = crearEventoConUnTipoDeEntrada(5);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        eventoService.descontarCupo(List.of(new DescontarCupoRequest(idTipoEntrada, 3)));

        DisponibilidadDTO disponibilidad = eventoService.consultarDisponibilidad(idTipoEntrada);
        assertThat(disponibilidad.cupoDisponible()).isEqualTo(2);
    }

    @Test
    void unDescuentoMayorAlDisponibleLanzaCupoInsuficienteYNoDejaCambios() {
        EventoDTO evento = crearEventoConUnTipoDeEntrada(5);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        assertThatThrownBy(() ->
                eventoService.descontarCupo(List.of(new DescontarCupoRequest(idTipoEntrada, 6))))
                .isInstanceOf(CupoInsuficienteException.class)
                .satisfies(ex -> {
                    CupoInsuficienteException cupoInsuficiente = (CupoInsuficienteException) ex;
                    assertThat(cupoInsuficiente.getIdTipoEntrada()).isEqualTo(idTipoEntrada);
                    assertThat(cupoInsuficiente.getCantidadPedida()).isEqualTo(6);
                    assertThat(cupoInsuficiente.getCupoDisponible()).isEqualTo(5);
                });

        DisponibilidadDTO disponibilidad = eventoService.consultarDisponibilidad(idTipoEntrada);
        assertThat(disponibilidad.cupoDisponible()).isEqualTo(5);
    }

    /**
     * El caso central para la defensa de PAS-8: un lote de dos lineas donde la primera tiene
     * cupo de sobra y la segunda lo excede. {@code descontarCupo} las aplica en orden y hace
     * {@code flush()} despues de cada una (para que la primera realmente llegue a la base antes
     * de que la segunda falle), pero el metodo entero sigue siendo una sola transaccion
     * declarativa: cuando la segunda linea lanza {@link CupoInsuficienteException}, todo el
     * metodo revierte — incluida la primera linea, que ya estaba escrita.
     *
     * <p>Es exactamente el mecanismo que {@code ServicioDeVentas.confirmarCompra} va a
     * demostrar en vivo (carrito con dos lineas, la segunda sin cupo, y el cupo de la primera
     * vuelve a su valor original), probado aca sin depender de que Ventas exista todavia.
     */
    @Test
    void unLoteConUnaLineaInsuficienteRevierteTambienLasLineasValidasDelMismoLote() {
        EventoDTO evento = crearEventoConDosTiposDeEntrada(5, 2);
        Long idConCupoDeSobra = evento.tiposEntrada().get(0).id();
        Long idSinCupo = evento.tiposEntrada().get(1).id();

        assertThatThrownBy(() -> eventoService.descontarCupo(List.of(
                new DescontarCupoRequest(idConCupoDeSobra, 3),
                new DescontarCupoRequest(idSinCupo, 5))))
                .isInstanceOf(CupoInsuficienteException.class);

        // Las dos lineas quedan sin cambios: la transaccion revirtio tambien la primera, que ya
        // habia hecho flush antes de que la segunda reventara.
        assertThat(eventoService.consultarDisponibilidad(idConCupoDeSobra).cupoDisponible())
                .isEqualTo(5);
        assertThat(eventoService.consultarDisponibilidad(idSinCupo).cupoDisponible())
                .isEqualTo(2);
    }

    /**
     * Dispara una carrera real entre dos descuentos concurrentes sobre el mismo tipo de
     * entrada: dos hilos liberados a la vez por un {@link CyclicBarrier}, cada uno pidiendo 3 de
     * un cupo de 5 (3+3=6 &gt; 5, pero ninguno lo ve venir porque cada uno lee el cupo *antes*
     * de que el otro escriba). Postgres serializa las dos escrituras por el lock de fila: la
     * segunda en llegar encuentra la version ya cambiada y Hibernate la traduce a
     * {@code ObjectOptimisticLockingFailureException}, no a una sobreventa silenciosa.
     *
     * <p>Es el {@code @Version} de {@code TipoEntrada}, puesto desde antes de que Ventas
     * existiera "porque ServicioDeVentas va a decrementar cupoDisponible con concurrencia
     * real", puesto a prueba de verdad.
     */
    @Test
    void dosDescuentosConcurrentesSobreElMismoTipoDejanUnaSolaEscrituraConsistente() throws Exception {
        EventoDTO evento = crearEventoConUnTipoDeEntrada(5);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Exception> intentoDeDescuento = () -> {
            barrera.await();
            try {
                eventoService.descontarCupo(List.of(new DescontarCupoRequest(idTipoEntrada, 3)));
                return null;
            } catch (Exception ex) {
                return ex;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Exception>> futuros;
        try {
            futuros = executor.invokeAll(List.of(intentoDeDescuento, intentoDeDescuento));
        } finally {
            executor.shutdown();
        }

        List<Exception> resultados = new ArrayList<>();
        for (Future<Exception> futuro : futuros) {
            resultados.add(futuro.get());
        }

        long exitosos = resultados.stream().filter(Objects::isNull).count();
        Exception fallo = resultados.stream().filter(Objects::nonNull).findFirst().orElse(null);

        assertThat(exitosos).isEqualTo(1);
        assertThat(fallo).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // El unico ganador dejo el cupo en 2 (5 - 3): ni en 5 (nadie escribio) ni en -1 (los dos
        // escribieron sin verse).
        assertThat(eventoService.consultarDisponibilidad(idTipoEntrada).cupoDisponible())
                .isEqualTo(2);
    }

    private EventoDTO crearEventoConUnTipoDeEntrada(int cupoTotal) {
        return crearEvento(List.of(new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), cupoTotal)));
    }

    private EventoDTO crearEventoConDosTiposDeEntrada(int cupoUno, int cupoDos) {
        return crearEvento(List.of(
                new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), cupoUno),
                new CrearTipoEntradaRequest("VIP", new BigDecimal("2000.00"), cupoDos)));
    }

    private EventoDTO crearEvento(List<CrearTipoEntradaRequest> tiposEntrada) {
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

        return eventoService.crearEvento(new CrearEventoRequest(
                idProductora,
                "Evento de Prueba " + sufijo,
                "Fixture de DescuentoDeCupoTest",
                OffsetDateTime.now().plusMonths(1),
                "Lugar de Prueba",
                tiposEntrada
        ), organizador.id());
    }
}
