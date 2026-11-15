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
        this("No hay cupo suficiente para \"" + nombreTipoEntrada + "\" (id " + idTipoEntrada
                + "): se pidieron " + cantidadPedida + " y quedan " + cupoDisponible,
                idTipoEntrada, cantidadPedida, cupoDisponible);
    }

    private CupoInsuficienteException(
            String mensaje, Long idTipoEntrada, int cantidadPedida, int cupoDisponible
    ) {
        super(mensaje);
        this.idTipoEntrada = idTipoEntrada;
        this.cantidadPedida = cantidadPedida;
        this.cupoDisponible = cupoDisponible;
    }

    /**
     * El evento no admite ventas en su estado actual (borrador o cancelado, PAS-35): para quien
     * compra no queda nada disponible, sin importar cuanto cupo figure. Reusar esta excepcion y no
     * crear otra es deliberado: Ventas ya la traduce a 409 y no necesita conocer los estados de un
     * evento.
     */
    public static CupoInsuficienteException porEventoSinVentas(
            Long idTipoEntrada, String nombreTipoEntrada, int cantidadPedida, EstadoEvento estado
    ) {
        return new CupoInsuficienteException(
                "\"" + nombreTipoEntrada + "\" (id " + idTipoEntrada + ") no esta a la venta: el evento esta "
                        + estado, idTipoEntrada, cantidadPedida, 0);
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
