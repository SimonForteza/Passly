package com.passly.ventas;

/** Se intento confirmar una compra sin haber agregado ninguna linea al carrito. */
public class CarritoVacioException extends RuntimeException {

    public CarritoVacioException() {
        super("El carrito no tiene items para confirmar");
    }
}
