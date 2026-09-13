package com.passly.ventas;

/**
 * Quien opera no es el comprador dueño del carrito de esta sesion.
 *
 * <p>Con {@code SessionCreationPolicy.STATELESS} nada asocia el {@code JSESSIONID} al principal
 * autenticado: si alguien reutilizara la cookie de sesion de otro junto con sus propias
 * credenciales Basic, veria el carrito ajeno. El carrito guarda el id del comprador en su
 * primera operacion y esta excepcion es la que convierte ese riesgo en una regla explicita
 * (CLAUDE.md 4.7), en vez de dejar que la sesion se comparta en silencio.
 */
public class CarritoDeOtroCompradorException extends RuntimeException {

    public CarritoDeOtroCompradorException() {
        super("Este carrito pertenece a otro comprador");
    }
}
