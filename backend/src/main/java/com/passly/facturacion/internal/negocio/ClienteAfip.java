package com.passly.facturacion.internal.negocio;

/** Puerto de integracion fiscal. El adapter SOAP se implementa en la Obligatoria 2. */
public interface ClienteAfip {
    void solicitarFacturacion(Long idOrden);
}
