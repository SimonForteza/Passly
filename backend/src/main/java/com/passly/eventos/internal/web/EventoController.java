package com.passly.eventos.internal.web;

import com.passly.eventos.EventoService;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link EventoService}. Sin logica de dominio y sin conocer
 * entidades: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
 *
 * <p><b>Sobre {@code idUsuarioActuante}:</b> es la identidad de quien opera, y viaja aparte de la
 * solicitud (nunca adentro), tal como {@link EventoService} lo pide. Desde PAS-6 sale de
 * {@link Authentication#getName()}: el {@code UserDetails} que arma el modulo {@code seguridad}
 * usa el id numerico como username (no el email), asi que este controller lo obtiene con un tipo
 * de Spring Security, sin depender de Usuarios para resolver email a id — dependencia que su
 * {@code package-info} no declara y que haria fallar el build.
 *
 * <p>Los endpoints de <b>lectura publica</b> no lo piden: la cartelera es de acceso libre, que es
 * justamente el punto de un marketplace.
 *
 * <p>Package-private: nada fuera de este paquete necesita nombrarla, Spring la registra igual
 * como bean {@code @RestController}.
 */
@RestController
class EventoController {

    private final EventoService eventoService;

    EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping("/api/eventos")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    ResponseEntity<EventoDTO> crearEvento(
            Authentication authentication,
            @Valid @RequestBody CrearEventoRequest solicitud
    ) {
        EventoDTO creado = eventoService.crearEvento(solicitud, idDelActuante(authentication));
        return ResponseEntity.created(URI.create("/api/eventos/" + creado.id())).body(creado);
    }

    @GetMapping("/api/eventos/{id}")
    EventoDTO consultarEvento(@PathVariable("id") Long id) {
        return eventoService.consultarEvento(id);
    }

    /**
     * Publicar una accion de negocio con reglas (valida fecha y cupos), no la escritura de un
     * campo: por eso {@code POST .../publicacion} y no {@code PATCH} con
     * {@code {"estado":"PUBLICADO"}}. Un PATCH habria sido igual de valido en REST puro, pero
     * comunica peor que se esta pidiendo una transicion, no un update.
     */
    @PostMapping("/api/eventos/{id}/publicacion")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    EventoDTO publicarEvento(@PathVariable("id") Long id, Authentication authentication) {
        return eventoService.publicarEvento(id, idDelActuante(authentication));
    }

    /**
     * Cartelera publica, opcionalmente filtrada por productora.
     *
     * <p>El filtro es un {@code @RequestParam} opcional sobre la misma coleccion y no un endpoint
     * aparte: {@code /api/eventos?productora=1} sigue siendo "los eventos publicados", con un
     * criterio de seleccion encima. Un path distinto sugeriria que es otro recurso.
     */
    @GetMapping("/api/eventos")
    List<EventoDTO> listarEventosPublicados(
            @RequestParam(name = "productora", required = false) Long idProductora
    ) {
        return idProductora == null
                ? eventoService.listarEventosPublicados()
                : eventoService.listarEventosPublicadosDeProductora(idProductora);
    }

    /**
     * Backoffice de la productora: sus eventos, <b>incluidos los borradores</b>.
     *
     * <p>Es un sub-recurso de la productora porque lo que se pide es "los eventos de esta
     * productora", no "los eventos filtrados". La diferencia con el filtro de arriba no es de forma
     * sino de contenido y de permisos: aca hay borradores y hace falta gestionar la productora.
     *
     * <p>Que el path empiece con {@code /api/productoras} y lo sirva el controlador de Eventos no es
     * una inconsistencia: la URL describe la jerarquia del recurso, no que componente lo resuelve.
     * Los eventos son de Eventos aunque cuelguen de una productora.
     */
    @GetMapping("/api/productoras/{idProductora}/eventos")
    List<EventoDTO> listarEventosDeProductora(
            @PathVariable("idProductora") Long idProductora,
            Authentication authentication
    ) {
        return eventoService.listarEventosDeProductora(idProductora, idDelActuante(authentication));
    }

    @GetMapping("/api/tipos-entrada/{id}/disponibilidad")
    DisponibilidadDTO consultarDisponibilidad(@PathVariable("id") Long id) {
        return eventoService.consultarDisponibilidad(id);
    }

    /**
     * El {@code UserDetails} de {@code seguridad} usa el id numerico como username (ver
     * {@code DetalleDeUsuarioParaAutenticacion}), asi que {@link Authentication#getName()} ya es el
     * id del actuante — sin resolverlo contra Usuarios, que este modulo no puede importar.
     */
    private static Long idDelActuante(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
