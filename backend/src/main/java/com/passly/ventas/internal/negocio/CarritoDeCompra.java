package com.passly.ventas.internal.negocio;

import com.passly.ventas.CarritoDeOtroCompradorException;
import com.passly.ventas.CarritoVacioException;
import com.passly.ventas.CarritoVencidoException;
import com.passly.ventas.dto.CarritoDTO;
import com.passly.ventas.dto.ItemDeCarritoDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b>El componente stateful de Passly (CLAUDE.md 4.7).</b> Guarda la seleccion parcial del
 * comprador mientras dura una compra: crece llamada a llamada y vive en memoria, no en disco.
 * Persistir datos no vuelve stateful a un componente; esto si lo es, porque el estado que
 * importa es conversacional y desaparece con la sesion.
 *
 * <p><b>{@code @SessionScope}: una instancia por sesion HTTP, administrada por el contenedor.</b>
 * {@link #alCrear()} y {@link #alDestruir()} son la evidencia en vivo de que Spring — no el
 * codigo de negocio — decide cuando nace y cuando muere este objeto.
 *
 * <p><b>No reserva cupo.</b> El precio se congela al agregar una linea (para que un cambio de
 * precio despues no afecte lo ya elegido), pero la cantidad <b>no se valida contra el cupo real</b>
 * en este punto — eso pasa recien al confirmar, dentro de la transaccion de
 * {@code ConfirmacionDeCompra}. Es el trade-off declarado en CLAUDE.md 4.7: en produccion este
 * carrito se persistiria (base o cache) para poder escalar horizontalmente y dejaria de ser
 * stateful; aca se elige la variante en memoria a proposito, para demostrar gestion de ciclo de
 * vida por contenedor.
 *
 * <p><b>Dos relojes distintos, no uno redundante.</b> {@link #expiraEn} es la regla de
 * <i>negocio</i>: absoluta desde que se creo el carrito, la valida {@link #itemsParaConfirmar}.
 * El {@code server.servlet.session.timeout} de {@code application.yml} es la limpieza del
 * <i>contenedor</i>: decide cuando Tomcat destruye la sesion (y con ella, esta instancia), no
 * cuando el negocio considera vencida la oferta. Por eso {@link #alDestruir()} puede loguearse
 * bastante despues de que {@link #expiraEn} ya paso — el reaper de sesiones no corre al segundo.
 *
 * <p><b>Sobre {@link #verificarDueño}:</b> con {@code SessionCreationPolicy.STATELESS} nada
 * asocia el {@code JSESSIONID} al principal autenticado (ver el Javadoc de
 * {@code ConfiguracionDeSeguridad}). Este guard es lo que convierte ese riesgo en una regla
 * explicita: el carrito recuerda quien lo empezo y lo exige en cada operacion.
 */
@Component
@SessionScope
class CarritoDeCompra {

    private static final Logger log = LoggerFactory.getLogger(CarritoDeCompra.class);
    private static final Duration DURACION_DEL_HOLD_POR_DEFECTO = Duration.ofMinutes(5);

    private final List<ItemDeCarritoDTO> items = new ArrayList<>();
    private final OffsetDateTime creadoEn = OffsetDateTime.now();
    private final OffsetDateTime expiraEn;
    private Long idComprador;

    @Autowired
    CarritoDeCompra() {
        this(DURACION_DEL_HOLD_POR_DEFECTO);
    }

    /**
     * Constructor con la duracion explicita, para poder probar el vencimiento sin esperar 5
     * minutos reales. {@code @Autowired} marca cual de los dos usa Spring; este otro solo lo usan
     * los tests (mismo paquete, visibilidad package-private).
     */
    CarritoDeCompra(Duration duracionDelHold) {
        this.expiraEn = creadoEn.plus(duracionDelHold);
    }

    @PostConstruct
    void alCrear() {
        log.info("Carrito creado por el contenedor; expira a las {}", expiraEn);
    }

    @PreDestroy
    void alDestruir() {
        log.info("Carrito destruido por el contenedor con {} linea(s) sin confirmar", items.size());
    }

    /**
     * Agrega una linea, o suma la cantidad si ya habia una del mismo tipo de entrada. El precio
     * y el nombre llegan ya resueltos (por {@code EventoService.consultarDisponibilidad}, desde
     * {@code VentaServiceImpl}): este objeto no conoce a Eventos, solo guarda lo que le pasan.
     */
    void agregarItem(
            Long idComprador, Long idTipoEntrada, Long idEvento,
            String nombreTipoEntrada, BigDecimal precioUnitario, int cantidad
    ) {
        verificarDueño(idComprador);

        Optional<ItemDeCarritoDTO> existente = items.stream()
                .filter(item -> item.idTipoEntrada().equals(idTipoEntrada))
                .findFirst();

        if (existente.isPresent()) {
            ItemDeCarritoDTO anterior = existente.get();
            items.remove(anterior);
            items.add(new ItemDeCarritoDTO(
                    idTipoEntrada, idEvento, nombreTipoEntrada, precioUnitario,
                    anterior.cantidad() + cantidad));
        } else {
            items.add(new ItemDeCarritoDTO(
                    idTipoEntrada, idEvento, nombreTipoEntrada, precioUnitario, cantidad));
        }
    }

    /**
     * Foto del carrito. Nunca lanza por vencimiento: siempre hay algo que mostrar, incluida la
     * cuenta regresiva llegando a cero (ver Javadoc de {@code CarritoDTO}).
     */
    CarritoDTO verComo(Long idComprador) {
        verificarDueño(idComprador);
        boolean expirado = haVencido();
        long segundosRestantes = expirado
                ? 0
                : Duration.between(OffsetDateTime.now(), expiraEn).toSeconds();
        return new CarritoDTO(List.copyOf(items), total(), expiraEn, segundosRestantes, expirado);
    }

    BigDecimal total() {
        return items.stream()
                .map(item -> item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Las lineas para confirmar la compra, <b>sin vaciar el carrito todavia</b>: si el cobro
     * falla despues de esta llamada, el carrito tiene que seguir intacto para poder reintentar.
     * Quien llama vacia explicitamente con {@link #vaciar} solo si la compra se confirma.
     *
     * @throws CarritoVacioException  si no hay lineas
     * @throws CarritoVencidoException si pasaron mas de 5 minutos desde que se creo
     */
    List<ItemDeCarritoDTO> itemsParaConfirmar(Long idComprador) {
        verificarDueño(idComprador);
        if (items.isEmpty()) {
            throw new CarritoVacioException();
        }
        if (haVencido()) {
            throw new CarritoVencidoException();
        }
        return List.copyOf(items);
    }

    void vaciar(Long idComprador) {
        verificarDueño(idComprador);
        items.clear();
    }

    private boolean haVencido() {
        return OffsetDateTime.now().isAfter(expiraEn);
    }

    private void verificarDueño(Long idComprador) {
        if (this.idComprador == null) {
            this.idComprador = idComprador;
            return;
        }
        if (!this.idComprador.equals(idComprador)) {
            throw new CarritoDeOtroCompradorException();
        }
    }
}
