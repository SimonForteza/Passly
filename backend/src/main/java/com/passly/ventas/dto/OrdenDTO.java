package com.passly.ventas.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Una compra confirmada: el comprador, sus lineas congeladas y el total cobrado. */
public record OrdenDTO(
        Long id,
        CompradorDeOrdenDTO comprador,
        List<ItemDeOrdenDTO> items,
        BigDecimal total,
        OffsetDateTime creadaEn
) {
}
