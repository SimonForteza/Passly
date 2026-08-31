package com.passly.eventos.dto;

import java.math.BigDecimal;

/**
 * Vista de salida de un tipo de entrada, siempre dentro de un {@link EventoDTO}.
 */
public record TipoEntradaDTO(
        Long id,
        String nombre,
        BigDecimal precio,
        Integer cupoTotal,
        Integer cupoDisponible
) {
}
