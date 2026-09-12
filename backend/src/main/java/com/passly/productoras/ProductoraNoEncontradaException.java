package com.passly.productoras;

/**
 * No existe una productora con el identificador pedido.
 *
 * <p>Es publica y vive en la raiz del modulo porque las excepciones de dominio son parte del
 * contrato: quien llame a {@link ProductoraService} tiene que poder distinguir "no existe" de
 * cualquier otro fallo. Se mapea a 404.
 *
 * <p>Extiende {@code RuntimeException} para que {@code @Transactional} revierta la transaccion por
 * defecto, sin necesidad de declarar {@code rollbackFor}.
 */
public class ProductoraNoEncontradaException extends RuntimeException {

    private final Long idProductora;

    public ProductoraNoEncontradaException(Long idProductora) {
        super("No existe una productora con id " + idProductora);
        this.idProductora = idProductora;
    }

    public Long getIdProductora() {
        return idProductora;
    }
}
