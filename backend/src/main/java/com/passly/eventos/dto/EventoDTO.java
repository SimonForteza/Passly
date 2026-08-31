package com.passly.eventos.dto;

import com.passly.eventos.EstadoEvento;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Vista de salida de un evento: lo que el componente expone al mundo.
 *
 * <p>Es deliberadamente distinto de la entidad {@code Evento}. Si se serializara la entidad,
 * la forma de la API quedaria atada a decisiones de mapeo ORM y cualquier renombre de columna
 * romperia a sus consumidores.
 */
public record EventoDTO(
        Long id,
        String nombre,
        String descripcion,
        OffsetDateTime fechaHora,
        String lugar,
        EstadoEvento estado,
        List<TipoEntradaDTO> tiposEntrada
) {
}
