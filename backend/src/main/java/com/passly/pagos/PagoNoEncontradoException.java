package com.passly.pagos;

/**
 * No existe un pago con el id pedido. Distinta de {@link ReversionNoDisponibleException}: esta
 * responde "¿existe?", aquella responde "¿admite la operación en su estado actual?".
 */
public class PagoNoEncontradoException extends RuntimeException {

    public PagoNoEncontradoException(Long idPago) {
        super("No existe un pago con id " + idPago);
    }
}
