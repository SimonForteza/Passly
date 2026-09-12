package com.passly.pagos;

/**
 * La pasarela de pago <b>no respondio</b>: caida, timeout, o conexion rechazada. A diferencia de
 * {@link PagoRechazadoException}, aca no hay ningun veredicto de negocio — el intento ni siquiera
 * llego a procesarse del otro lado.
 *
 * <p>Se mapea a <b>503 Service Unavailable</b>, no a un 500 generico: el problema es un externo
 * caido, no un bug de Passly. Es la misma logica que CLAUDE.md aplica a AFIP en el flujo de
 * ventas — "la venta ya esta confirmada y el mensaje se reintenta" — aplicada aca al cobro: que la
 * pasarela este caida no tiene por que tumbar el resto del sistema, y es la base para el punto
 * extra de resiliencia (CLAUDE.md 7).
 */
public class PasarelaDePagoNoDisponibleException extends RuntimeException {

    public PasarelaDePagoNoDisponibleException(String referencia, Throwable causa) {
        super("La pasarela de pago no respondio para la referencia " + referencia, causa);
    }
}
