package com.passly.ventas;

/**
 * No existe una orden con ese id, <b>o existe pero no es del comprador que pregunta</b>.
 *
 * <p>A proposito una sola excepcion para los dos casos, mapeada a <b>404, no 403</b>: a
 * diferencia de una productora (publica, en una cartelera), una orden ajena no deberia siquiera
 * confirmar que existe. Contrastar con {@code ProductoraNoGestionableException}, que si es 403
 * porque ahi el recurso es publico y no hay nada que ocultar.
 */
public class OrdenNoEncontradaException extends RuntimeException {

    private final Long idOrden;

    public OrdenNoEncontradaException(Long idOrden) {
        super("No existe una orden con id " + idOrden);
        this.idOrden = idOrden;
    }

    public Long getIdOrden() {
        return idOrden;
    }
}
