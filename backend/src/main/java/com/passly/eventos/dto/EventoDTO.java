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
 *
 * <p><b>El {@code organizador} no sale de la entidad.</b> {@code Evento} solo guarda un
 * {@code productoraId}; el nombre comercial lo resuelve el servicio preguntandole a Productoras. Es
 * la composicion en memoria que reemplaza al join entre esquemas que CLAUDE.md 4.8 prohibe — y es
 * justamente lo que le permite al comprador ver de quien es cada fiesta.
 */
public record EventoDTO(
        Long id,
        OrganizadorDeEventoDTO organizador,
        String nombre,
        String descripcion,
        OffsetDateTime fechaHora,
        String lugar,
        EstadoEvento estado,
        List<TipoEntradaDTO> tiposEntrada
) {
}
