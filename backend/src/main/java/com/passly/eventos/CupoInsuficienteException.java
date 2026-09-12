package com.passly.eventos;

/**
 * No queda cupo suficiente de un tipo de entrada para la cantidad pedida.
 *
 * <p>Es la ultima linea de defensa contra la sobreventa: {@code ServicioDeVentas} ya valido
 * disponibilidad al armar el carrito, pero esta excepcion cubre la carrera entre dos compras
 * concurrentes sobre el mismo tipo de entrada (CLAUDE.md 4.7 — el carrito es solo estado
 * conversacional, no reserva cupo).
 *
 * <p>Extiende {@code RuntimeException} para que {@code @Transactional} revierta la transaccion
 * por defecto, sin necesidad de declarar {@code rollbackFor}.
 */
public class CupoInsuficienteException extends RuntimeException {

    private final Long idTipoEntrada;
    private final int cantidadPedida;
    private final int cupoDisponible;

    public CupoInsuficienteException(
            Long idTipoEntrada, String nombreTipoEntrada, int cantidadPedida, int cupoDisponible
    ) {
        super("No hay cupo suficiente para \"" + nombreTipoEntrada + "\" (id " + idTipoEntrada
                + "): se pidieron " + cantidadPedida + " y quedan " + cupoDisponible);
        this.idTipoEntrada = idTipoEntrada;
        this.cantidadPedida = cantidadPedida;
        this.cupoDisponible = cupoDisponible;
    }

    public Long getIdTipoEntrada() {
        return idTipoEntrada;
    }

    public int getCantidadPedida() {
        return cantidadPedida;
    }

    public int getCupoDisponible() {
        return cupoDisponible;
    }
}
