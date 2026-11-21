package com.passly.eventos.internal.negocio;

import com.passly.eventos.ArchivoDePortadaInvalidoException;

/**
 * Decide si un archivo sirve como portada y de que tipo es (PAS-47).
 *
 * <p><b>El tipo sale de los primeros bytes del archivo</b> (su "firma"), no del nombre ni del
 * {@code Content-Type} que manda el navegador: los dos los elige el cliente y se pueden falsear.
 * Asi, un HTML o un SVG renombrado a {@code .jpg} no llega a guardarse ni a servirse como imagen.
 * Se aceptan solo formatos raster; SVG queda afuera porque puede llevar scripts.
 */
final class ValidadorDePortada {

    static final int TAMANIO_MAXIMO = 2 * 1024 * 1024;

    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] FIRMA_WEBP = {'W', 'E', 'B', 'P'};

    private ValidadorDePortada() {
    }

    /**
     * @return el tipo de contenido detectado ({@code image/jpeg}, {@code image/png} o {@code image/webp})
     * @throws ArchivoDePortadaInvalidoException si esta vacio, supera 2 MB o no es una imagen admitida
     */
    static String tipoDe(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new ArchivoDePortadaInvalidoException("El archivo esta vacio");
        }
        if (contenido.length > TAMANIO_MAXIMO) {
            throw new ArchivoDePortadaInvalidoException("La imagen supera el maximo de 2 MB");
        }
        if (empiezaCon(contenido, FIRMA_JPEG, 0)) {
            return "image/jpeg";
        }
        if (empiezaCon(contenido, FIRMA_PNG, 0)) {
            return "image/png";
        }
        if (empiezaCon(contenido, FIRMA_RIFF, 0) && empiezaCon(contenido, FIRMA_WEBP, 8)) {
            return "image/webp";
        }
        throw new ArchivoDePortadaInvalidoException("La portada tiene que ser una imagen JPG, PNG o WebP");
    }

    private static boolean empiezaCon(byte[] contenido, byte[] firma, int desde) {
        if (contenido.length < desde + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[desde + i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
