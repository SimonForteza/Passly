package com.passly.ventas.internal.negocio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Lo que devuelve un cobro aprobado. Puramente interno: no forma parte del contrato publico de
 * Ventas — {@code OrdenDTO} no lo expone tal cual, solo lo referencia en la orden persistida.
 */
record ComprobanteDeCobroDTO(String id, BigDecimal importe, OffsetDateTime autorizadoEn) {
}
