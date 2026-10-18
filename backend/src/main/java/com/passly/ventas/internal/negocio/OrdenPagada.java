package com.passly.ventas.internal.negocio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Snapshot interno. El consumidor tiene su propio record: el contrato compartido es JSON. */
record OrdenPagada(Long idOrden, Long idComprador, BigDecimal total, OffsetDateTime cobradaEn) {}
