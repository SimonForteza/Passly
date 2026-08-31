package com.passly.eventos;

import com.passly.eventos.dto.CrearEventoRequest;
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
 * <p><b>Posicion en el sistema:</b> Eventos es la raiz del grafo de dependencias — no llama a
 * ningun otro componente. {@code EstructuraDeModulosTest} verifica esa afirmacion en cada build.
 *
 * <p><b>No es un Facade.</b> Es una interfaz de servicio: no oculta la coordinacion de varios
 * subsistemas porque no hay ninguno que coordinar. El Facade del sistema es
 * {@code ServicioDeVentas}, que si orquesta Eventos, Usuarios, Pagos y Tickets.
 */
public interface EventoService {

    /**
     * Da de alta un evento con sus tipos de entrada. Nace en {@link EstadoEvento#BORRADOR}:
     * no aparece en la cartelera hasta que se lo publique explicitamente.
     *
     * <p>Cada tipo de entrada arranca con {@code cupoDisponible == cupoTotal}.
     */
    EventoDTO crearEvento(CrearEventoRequest solicitud);

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
     * @throws EventoNoEncontradoException          si no existe
     * @throws TransicionDeEstadoInvalidaException  si el estado o los datos no permiten publicarlo
     */
    EventoDTO publicarEvento(Long idEvento);

    /**
     * Cartelera publica: los eventos en {@link EstadoEvento#PUBLICADO}, del mas proximo al mas
     * lejano. Los borradores y los cancelados no aparecen.
     */
    List<EventoDTO> listarEventosPublicados();

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
}
