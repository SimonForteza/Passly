package com.passly.ventas.dto;

import java.math.BigDecimal;

/**
 * Una linea del carrito, con el precio ya congelado al momento de agregarla: si el organizador
 * cambia el precio del tipo de entrada despues, esta linea no se entera.
 */
public record ItemDeCarritoDTO(
        Long idTipoEntrada,
        Long idEvento,
        String nombreTipoEntrada,
        BigDecimal precioUnitario,
        Integer cantidad
) {
}
