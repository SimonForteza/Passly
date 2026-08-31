package com.passly.eventos;

/**
 * No existe un tipo de entrada con el identificador pedido.
 *
 * <p>Parte del contrato del componente: {@code ServicioDeVentas} consulta disponibilidad por id
 * de tipo de entrada y necesita distinguir "ese tipo no existe" de "no hay cupo".
 */
public class TipoEntradaNoEncontradoException extends RuntimeException {

    private final Long idTipoEntrada;

    public TipoEntradaNoEncontradoException(Long idTipoEntrada) {
        super("No existe un tipo de entrada con id " + idTipoEntrada);
        this.idTipoEntrada = idTipoEntrada;
    }

    public Long getIdTipoEntrada() {
        return idTipoEntrada;
    }
}
