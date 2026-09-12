package com.passly.ventas;

/**
 * El carrito supero los ~5 minutos desde que se creo. Es la regla de negocio, distinta y mas
 * estricta que el timeout de sesion del contenedor: el reloj del carrito es absoluto desde su
 * creacion y se valida en cada operacion, mientras que el timeout de sesion solo decide cuando
 * el contenedor libera la memoria (CLAUDE.md 4.7).
 *
 * <p>Extiende {@code RuntimeException} para que {@code @Transactional} revierta por defecto, sin
 * declarar {@code rollbackFor}.
 */
public class CarritoVencidoException extends RuntimeException {

    public CarritoVencidoException() {
        super("El carrito vencio; hay que armarlo de nuevo");
    }
}
