package com.passly.ventas.internal.negocio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Snapshot interno; JSON desacopla al productor del consumidor de facturación. */
record OrdenPagada(Long idOrden, Long idComprador, BigDecimal total, OffsetDateTime cobradaEn,
                   String compradorNombre, String compradorEmail) {
    OrdenPagada(Long idOrden, Long idComprador, BigDecimal total, OffsetDateTime cobradaEn) {
        this(idOrden,idComprador,total,cobradaEn,null,null);
    }
}
