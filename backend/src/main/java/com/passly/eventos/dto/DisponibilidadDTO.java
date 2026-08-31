package com.passly.eventos.dto;

import java.math.BigDecimal;

/**
 * Respuesta de {@code consultarDisponibilidad}: el contrato pensado para
 * {@code ServicioDeVentas}.
 *
 * <p>Trae <b>precio y cupo juntos</b> a proposito. Ventas necesita los dos para armar una orden,
 * y devolverlos en una sola llamada evita que tenga que ir a buscar el precio por otro lado —
 * o peor, que haga un join contra el esquema {@code eventos}, que la regla 3 de CLAUDE.md 4.3
 * prohibe.
 */
public record DisponibilidadDTO(
        Long idTipoEntrada,
        Long idEvento,
        String nombreTipoEntrada,
        BigDecimal precio,
        Integer cupoDisponible,
        boolean hayDisponibilidad
) {
}
