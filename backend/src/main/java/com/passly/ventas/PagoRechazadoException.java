package com.passly.ventas;

/**
 * La pasarela de pago rechazo el cobro. El carrito sobrevive: quien compra puede corregir el
 * medio de pago y reintentar sin perder lo que ya habia elegido.
 */
public class PagoRechazadoException extends RuntimeException {

    public PagoRechazadoException(String motivo) {
        super("El pago fue rechazado: " + motivo);
    }
}
