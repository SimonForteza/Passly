package com.passly.eventos;

import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.DescontarCupoRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;

import java.util.List;

/**
 * Contrato publico de <b>ServicioDeEventos</b>: alta, publicacion y consulta de eventos y sus
 * tipos de entrada.
 *
 * <p>Es la unica puerta de entrada al componente. La implementacion, las entidades JPA y los
 * repositorios viven en {@code internal} y no forman parte de este contrato; los demas
 * componentes inyectan esta interfaz y no pueden nombrar la clase concreta.
 *
 * <p><b>Posicion en el sistema:</b> Eventos depende de Productoras — para saber quien puede
 * gestionar los eventos de cada una y con que nombre comercial mostrarlos — y de nadie mas. La
 * dependencia esta declarada en el {@code package-info} del modulo y el build falla si aparece
 * cualquier otra. No depende de Usuarios: la identidad queda detras de Productoras, que ya resolvio
 * el cruce de roles al armar su padron.
 *
 * <p><b>Sobre el {@code idUsuarioActuante} de las operaciones de gestion:</b> es quien opera, y
 * viaja como parametro <i>aparte</i> de la solicitud, nunca adentro. Hoy la capa web lo saca de un
 * header temporal y con Spring Security saldra del principal autenticado — y este contrato no cambia
 * en esa migracion, justamente por estar separado.
 *
 * <p><b>No es un Facade.</b> Es una interfaz de servicio: no oculta la coordinacion de varios
 * subsistemas porque no hay ninguno que coordinar. El Facade del sistema es
 * {@code ServicioDeVentas}, que si orquesta Eventos, Usuarios, Pagos y Tickets.
 */
public interface EventoService {

    /**
     * Da de alta un evento con sus tipos de entrada, a nombre de la productora indicada en la
     * solicitud. Nace en {@link EstadoEvento#BORRADOR}: no aparece en la cartelera hasta que se lo
     * publique explicitamente.
     *
     * <p>Cada tipo de entrada arranca con {@code cupoDisponible == cupoTotal}.
     *
     * @param idUsuarioActuante quien opera; tiene que poder gestionar los eventos de esa productora
     * @throws ProductoraNoGestionableException si no puede gestionarla, o si la productora no existe
     */
    EventoDTO crearEvento(CrearEventoRequest solicitud, Long idUsuarioActuante);

    /**
     * Devuelve un evento por id, cualquiera sea su estado.
     *
     * @throws EventoNoEncontradoException si no existe
     */
    EventoDTO consultarEvento(Long idEvento);

    /**
     * Publica un evento: {@code BORRADOR -> PUBLICADO}.
     *
     * <p>Valida que el evento este en borrador, que su fecha sea futura y que tenga al menos un
     * tipo de entrada con cupo disponible.
     *
     * @param idUsuarioActuante quien opera; tiene que poder gestionar la productora dueña del evento
     * @throws EventoNoEncontradoException         si no existe
     * @throws ProductoraNoGestionableException    si el evento es de una productora que no gestiona
     * @throws TransicionDeEstadoInvalidaException si el estado o los datos no permiten publicarlo
     */
    EventoDTO publicarEvento(Long idEvento, Long idUsuarioActuante);

    /**
     * Cartelera publica: los eventos en {@link EstadoEvento#PUBLICADO}, del mas proximo al mas
     * lejano. Los borradores y los cancelados no aparecen.
     */
    List<EventoDTO> listarEventosPublicados();

    /**
     * La cartelera publica de una sola productora: lo que ve el comprador cuando elige un
     * organizador. Solo eventos publicados, asi que no necesita identidad.
     */
    List<EventoDTO> listarEventosPublicadosDeProductora(Long idProductora);

    /**
     * Backoffice de la productora: <b>todos</b> sus eventos, incluidos los que estan en
     * {@link EstadoEvento#BORRADOR}.
     *
     * <p>Es la unica operacion de lectura que exige autorizacion, y por eso recibe quien pregunta:
     * el borrador de una fiesta todavia no anunciada es informacion comercial sensible. La cartelera
     * publica, en cambio, es de acceso libre.
     *
     * @param idUsuarioActuante quien opera; tiene que poder gestionar los eventos de esa productora
     * @throws ProductoraNoGestionableException si no puede gestionarla, o si la productora no existe
     */
    List<EventoDTO> listarEventosDeProductora(Long idProductora, Long idUsuarioActuante);

    /**
     * Precio y cupo disponible de un tipo de entrada.
     *
     * <p>Es la operacion que va a consumir {@code ServicioDeVentas} antes de retener entradas.
     * Existe para que ningun otro componente necesite leer el esquema {@code eventos}
     * directamente.
     *
     * @throws TipoEntradaNoEncontradoException si no existe
     */
    DisponibilidadDTO consultarDisponibilidad(Long idTipoEntrada);

    /**
     * Descuenta cupo de una o mas lineas, cada una identificada por su tipo de entrada. Pensada
     * para {@code ServicioDeVentas.confirmarCompra}: un solo cruce de frontera por toda la
     * compra, no uno por linea.
     *
     * <p>Se aplican <b>ordenadas por {@code idTipoEntrada}</b>, para que dos confirmaciones
     * concurrentes que comparten lineas las toquen siempre en el mismo orden y no se
     * deadlockeen entre si esperandose en orden inverso.
     *
     * <p>Si una linea falla, las anteriores <b>ya quedaron escritas</b> (no hay pre-validacion
     * de todo el lote): es la transaccion que envuelve a quien llama la que decide si eso se
     * revierte. Descontar sin pre-validar es deliberado — es la ultima linea de defensa contra
     * la carrera entre compras concurrentes, y pre-validar todo el lote la volveria inutil.
     *
     * @throws TipoEntradaNoEncontradoException si alguna linea no corresponde a un tipo de entrada real
     * @throws CupoInsuficienteException        si alguna linea pide mas de lo disponible
     */
    void descontarCupo(List<DescontarCupoRequest> lineas);
}
