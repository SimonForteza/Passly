package com.passly.ventas.dto;

import java.math.BigDecimal;

/**
 * Una linea confirmada. Es un snapshot historico, no una referencia en vivo a
 * {@code eventos.tipo_entrada}: lo que se cobro no cambia si el organizador ajusta el precio
 * despues.
 */
public record ItemDeOrdenDTO(
        Long idTipoEntrada,
        Long idEvento,
        String nombreTipoEntrada,
        BigDecimal precioUnitario,
        Integer cantidad
) {
}
