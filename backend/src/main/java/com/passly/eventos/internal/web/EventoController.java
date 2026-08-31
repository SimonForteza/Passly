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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link EventoService}. Sin logica de dominio y sin conocer
 * entidades: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
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
    ResponseEntity<EventoDTO> crearEvento(@Valid @RequestBody CrearEventoRequest solicitud) {
        EventoDTO creado = eventoService.crearEvento(solicitud);
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
    EventoDTO publicarEvento(@PathVariable("id") Long id) {
        return eventoService.publicarEvento(id);
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
