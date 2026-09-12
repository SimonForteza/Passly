package com.passly.ventas.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Foto del carrito de quien opera: sus lineas, el total, y cuanto le queda antes de que el
 * contenedor lo destruya.
 *
 * <p>{@code expirado} viaja explicito en vez de responder con un error: {@code GET} sobre el
 * carrito siempre devuelve 200, incluso vencido, para poder mostrar la cuenta regresiva llegando
 * a cero. La regla que de verdad importa (si se puede confirmar la compra) la impone
 * {@code confirmarCompra}, no esta consulta.
 */
public record CarritoDTO(
        List<ItemDeCarritoDTO> items,
        BigDecimal total,
        OffsetDateTime expiraEn,
        long segundosRestantes,
        boolean expirado
) {
}
