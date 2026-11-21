package com.passly.eventos;

/** El evento existe pero no tiene portada (PAS-47). La capa web lo traduce a 404. */
public class PortadaNoEncontradaException extends RuntimeException {

    public PortadaNoEncontradaException(Long idEvento) {
        super("El evento " + idEvento + " no tiene portada");
    }
}
