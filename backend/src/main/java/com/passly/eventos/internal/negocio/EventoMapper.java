package com.passly.eventos.internal.negocio;

import com.passly.eventos.dto.EventoDTO;
import com.passly.eventos.dto.TipoEntradaDTO;
import com.passly.eventos.internal.datos.Evento;
import com.passly.eventos.internal.datos.TipoEntrada;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entidad JPA -&gt; DTO. Es <b>el unico lugar del componente donde una entidad y un DTO se
 * tocan</b>: que sea un solo archivo es lo que hace auditable la regla "las entidades JPA nunca
 * salen del componente" (CLAUDE.md 4.3).
 *
 * <p>La alternativa descartada es serializar la entidad directo, tapando lo que sobre con
 * {@code @JsonIgnore}. Se descarta porque acopla la forma de la API a decisiones de mapeo ORM, y
 * porque con {@code open-in-view: false} intentar serializar una coleccion lazy fuera de la
 * transaccion directamente falla en vez de andar "por las dudas".
 */
@Component
class EventoMapper {

    EventoDTO aDTO(Evento evento) {
        List<TipoEntradaDTO> tipos = evento.getTiposEntrada().stream()
                .map(this::aDTO)
                .toList();

        return new EventoDTO(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFechaHora(),
                evento.getLugar(),
                evento.getEstado(),
                tipos
        );
    }

    TipoEntradaDTO aDTO(TipoEntrada tipoEntrada) {
        return new TipoEntradaDTO(
                tipoEntrada.getId(),
                tipoEntrada.getNombre(),
                tipoEntrada.getPrecio(),
                tipoEntrada.getCupoTotal(),
                tipoEntrada.getCupoDisponible()
        );
    }
}
