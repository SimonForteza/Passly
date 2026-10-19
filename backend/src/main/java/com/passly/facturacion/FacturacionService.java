package com.passly.facturacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Registra una factura pendiente de AFIP. Una orden tiene a lo sumo una factura. */
public interface FacturacionService {
    void registrarOrdenPagada(Long idOrden, Long idComprador, BigDecimal total, OffsetDateTime cobradaEn);
    default void registrarOrdenPagada(Long idOrden,Long idComprador,BigDecimal total,
            OffsetDateTime cobradaEn,String compradorNombre,String compradorEmail) {
        registrarOrdenPagada(idOrden,idComprador,total,cobradaEn);
    }
}
