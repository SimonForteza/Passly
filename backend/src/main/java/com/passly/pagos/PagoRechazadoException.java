package com.passly.pagos;

/**
 * La pasarela de pago proceso el pedido pero <b>rechazo</b> el cobro: fondos insuficientes,
 * tarjeta invalida, o cualquier otro motivo de negocio del lado del proveedor.
 *
 * <p>Se distingue a proposito de {@link PasarelaDePagoNoDisponibleException}: aca la pasarela
 * respondio y dijo que no, mientras que alla la pasarela no respondio en absoluto. La diferencia
 * es la que separa un <b>402 Payment Required</b> (el cobro se intento y no prospero) de un
 * <b>503 Service Unavailable</b> (el problema es tecnico y conviene reintentar). Confundirlas
 * haria que un comprador con la tarjeta rechazada reciba el mismo mensaje que uno cuya pasarela
 * esta caida, cuando la accion correcta es distinta en cada caso.
 *
 * <p>Extiende {@code RuntimeException} para que un eventual {@code @Transactional} que la
 * envuelva revierta por defecto, sin declarar {@code rollbackFor}.
 */
public class PagoRechazadoException extends RuntimeException {

    private final String referencia;
    private final String idTransaccionExterna;

    public PagoRechazadoException(String referencia, String idTransaccionExterna) {
        super("La pasarela de pago rechazo el cobro de la referencia " + referencia
                + " (transaccion externa " + idTransaccionExterna + ")");
        this.referencia = referencia;
        this.idTransaccionExterna = idTransaccionExterna;
    }

    public String getReferencia() {
        return referencia;
    }

    /** Id que la pasarela asigno igual, aunque haya rechazado el cobro: sirve para conciliar. */
    public String getIdTransaccionExterna() {
        return idTransaccionExterna;
    }
}
