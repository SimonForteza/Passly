package com.passly.eventos.internal.web;

import com.passly.eventos.EventoService;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link EventoService}. Sin logica de dominio y sin conocer
 * entidades: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
 *
 * <p><b>Sobre el header {@code X-Usuario-Id}:</b> es la identidad de quien opera, y es temporal —
 * Spring Security es PAS-6. Es deliberadamente falsificable y no pretende ser seguridad: lo que
 * logra es que el modelo de autorizacion ya este completo y probado cuando llegue la autenticacion.
 * Migrar es una linea por endpoint ({@code @RequestHeader} pasa a {@code @AuthenticationPrincipal})
 * sin tocar los DTOs ni las firmas de {@link EventoService}.
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
    ResponseEntity<EventoDTO> crearEvento(
            @RequestHeader("X-Usuario-Id") Long idUsuarioActuante,
            @Valid @RequestBody CrearEventoRequest solicitud
    ) {
        EventoDTO creado = eventoService.crearEvento(solicitud, idUsuarioActuante);
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
    EventoDTO publicarEvento(
            @PathVariable("id") Long id,
            @RequestHeader("X-Usuario-Id") Long idUsuarioActuante
    ) {
        return eventoService.publicarEvento(id, idUsuarioActuante);
    }

    @GetMapping("/api/eventos")
    List<EventoDTO> listarEventosPublicados() {
        return eventoService.listarEventosPublicados();
    }

    @GetMapping("/api/tipos-entrada/{id}/disponibilidad")
    DisponibilidadDTO consultarDisponibilidad(@PathVariable("id") Long id) {
        return eventoService.consultarDisponibilidad(id);
    }
}
