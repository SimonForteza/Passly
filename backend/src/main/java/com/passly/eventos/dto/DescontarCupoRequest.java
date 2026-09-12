package com.passly.eventos.dto;

/**
 * Una linea de descuento de cupo, pedida por {@code ServicioDeVentas} al confirmar una compra.
 *
 * <p>Sin validacion de Bean Validation: no es un request HTTP, es un contrato entre
 * componentes que ya validaron sus datos en su propia frontera.
 */
public record DescontarCupoRequest(Long idTipoEntrada, int cantidad) {
}
