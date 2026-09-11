package com.passly.productoras;

/**
 * Ya existe una productora con ese nombre comercial.
 *
 * <p>El recurso no falta: choca con uno que ya existe, asi que es <b>409 Conflict</b> y no 400 — el
 * mismo relato que {@code EmailYaRegistradoException} en Usuarios. El guard real es el
 * {@code unique} sobre la columna; esta excepcion hace la comprobacion explicita antes del insert
 * para devolver un error de dominio en vez de una violacion de integridad.
 *
 * <p>El nombre comercial es la identidad publica de la productora en la cartelera: si dos
 * productoras pudieran llamarse igual, un comprador no podria distinguir de quien es cada fiesta,
 * que es justamente el problema que este componente resuelve.
 */
public class NombreComercialYaRegistradoException extends RuntimeException {

    private final String nombreComercial;

    public NombreComercialYaRegistradoException(String nombreComercial) {
        super("Ya existe una productora registrada con el nombre comercial " + nombreComercial);
        this.nombreComercial = nombreComercial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }
}
