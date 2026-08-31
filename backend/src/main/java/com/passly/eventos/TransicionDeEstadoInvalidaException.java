package com.passly.eventos;

/**
 * Se intento una transicion de estado que las reglas del dominio no permiten: por ejemplo
 * publicar un evento que ya esta publicado, o publicar uno sin tipos de entrada.
 *
 * <p>Se distingue de {@link EventoNoEncontradoException} a proposito: el recurso existe, pero
 * su estado no admite la operacion. Esa diferencia es la que permite responder <b>409 Conflict</b>
 * en vez de 404, y es la evidencia mas visible de que hay una regla de negocio real detras del
 * endpoint.
 */
public class TransicionDeEstadoInvalidaException extends RuntimeException {

    public TransicionDeEstadoInvalidaException(String motivo) {
        super(motivo);
    }
}
