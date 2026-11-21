package com.passly.eventos;

/**
 * El archivo subido como portada no es una imagen JPEG, PNG o WebP, esta vacio o supera el tamaño
 * maximo (PAS-47). La capa web lo traduce a 400.
 */
public class ArchivoDePortadaInvalidoException extends RuntimeException {

    public ArchivoDePortadaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
