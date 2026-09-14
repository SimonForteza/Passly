package com.passly.eventos;

/**
 * Se intento editar un evento o uno de sus tipos de entrada con datos o en un estado que la
 * regla de negocio no admite: editar datos de un evento que ya no esta en {@code BORRADOR},
 * bajar el cupo total de un tipo de entrada por debajo de lo ya vendido, repetir el nombre de un
 * tipo de entrada dentro del mismo evento.
 *
 * <p>Se distingue de {@link TransicionDeEstadoInvalidaException} a proposito: esa es sobre el
 * estado del propio {@code Evento} ({@code BORRADOR -> PUBLICADO}); esta es sobre si una edicion
 * puntual es valida dado el estado o los datos actuales, sin que haya ninguna transicion de por
 * medio (PAS-19). Las dos mapean a <b>409 Conflict</b>: el recurso existe, pero la operacion pedida
 * no es valida en su situacion actual.
 */
public class EdicionDeEventoInvalidaException extends RuntimeException {

    public EdicionDeEventoInvalidaException(String motivo) {
        super(motivo);
    }
}
