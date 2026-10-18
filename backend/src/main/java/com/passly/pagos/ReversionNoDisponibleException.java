package com.passly.pagos;

/**
 * El pago existe pero su estado actual no admite revertirse: ya está {@code RECHAZADO} (nunca se
 * cobró) o ya está {@code REVERTIDO} (ya se compensó una vez). Distinta de
 * {@link PagoNoEncontradoException}: acá el pago existe, el problema es su estado.
 */
public class ReversionNoDisponibleException extends RuntimeException {

    public ReversionNoDisponibleException(Long idPago, EstadoDePago estadoActual) {
        super("El pago " + idPago + " no admite reversion: esta en estado " + estadoActual);
    }
}
