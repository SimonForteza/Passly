package com.passly.facturacion.internal.negocio;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** No emite comprobantes fiscales ni simula una respuesta de AFIP. */
@Component
class ClienteAfipPendiente implements ClienteAfip {
    public void solicitarFacturacion(Long idOrden) {
        LoggerFactory.getLogger(getClass()).info("Orden {} pendiente de integracion SOAP con AFIP", idOrden);
    }
}
