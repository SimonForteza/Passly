package com.passly.pagos;

/**
 * Resultado de negocio de un cobro contra la pasarela de pago.
 *
 * <p>Vive en la raiz del modulo y no en {@code internal.datos}, igual que
 * {@code eventos.EstadoEvento}: es <b>parte del contrato</b>, no un detalle de mapeo ORM.
 * {@code ServicioDeVentas} va a necesitar distinguir estos dos estados para decidir si confirma
 * la compra o la aborta.
 *
 * <p>Se mapea con {@code @Enumerated(EnumType.STRING)}, nunca {@code ORDINAL}, para que agregar
 * un estado nuevo no corrompa las filas ya guardadas.
 */
public enum EstadoDePago {

    /** La pasarela proceso el cobro y lo acepto. */
    APROBADO,

    /** La pasarela proceso el pedido pero rechazo el cobro (fondos, tarjeta invalida, etc.). */
    RECHAZADO
}
