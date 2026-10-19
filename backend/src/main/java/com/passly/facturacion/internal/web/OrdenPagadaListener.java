package com.passly.facturacion.internal.web;

import com.passly.facturacion.FacturacionService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** El ACK JMS ocurre despues del commit de registrarOrdenPagada. Si falla, hay redelivery. */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "passly.facturacion.consumidor-activo", havingValue = "true", matchIfMissing = true)
class OrdenPagadaListener {
    private final FacturacionService facturacion;
    private final JsonMapper mapper;
    OrdenPagadaListener(FacturacionService facturacion, JsonMapper mapper) {
        this.facturacion = facturacion;
        this.mapper = mapper;
    }
    @JmsListener(id = "facturacion-orden-pagada", destination = "orden.pagada")
    public void recibir(String json) {
        OrdenPagada mensaje = mapper.readValue(json, OrdenPagada.class);
        facturacion.registrarOrdenPagada(mensaje.idOrden(),mensaje.idComprador(),mensaje.total(),
                mensaje.cobradaEn(),mensaje.compradorNombre(),mensaje.compradorEmail());
    }
    // Tipo local: no importar eventos ni DTOs de ventas.
    record OrdenPagada(Long idOrden, Long idComprador, BigDecimal total, OffsetDateTime cobradaEn,
                        String compradorNombre, String compradorEmail) {}
}
