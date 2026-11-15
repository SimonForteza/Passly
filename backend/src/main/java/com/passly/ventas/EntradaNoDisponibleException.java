package com.passly.ventas;

/**
 * El tipo de entrada no se puede vender ahora: su evento no admite ventas (no esta
 * {@code PUBLICADO}: es un borrador o fue cancelado) o ya no le queda cupo (PAS-37, DEF-001).
 *
 * <p><b>Mensaje generico a proposito.</b> No dice si el evento es un borrador, esta cancelado o
 * se agoto: quien adivina el id de un tipo de entrada de una fiesta no anunciada no tiene que
 * enterarse de que existe (mismo criterio que el 404 de los borradores en PAS-25). Se responde
 * <b>409</b> y no 404 porque el tipo de entrada si existe y la disponibilidad publica ya lo
 * expone; lo que se rechaza es la operacion en el estado actual.
 */
public class EntradaNoDisponibleException extends RuntimeException {

    public EntradaNoDisponibleException(Long idTipoEntrada) {
        super("La entrada " + idTipoEntrada + " no esta disponible para la venta");
    }
}
