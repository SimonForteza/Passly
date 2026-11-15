package com.passly.eventos;

/**
 * Ciclo de vida de un evento.
 *
 * <p>Vive en la raiz del modulo y no en {@code internal.datos} porque es <b>parte del
 * contrato</b>: {@code ServicioDeVentas} va a necesitar saber si un evento esta publicado
 * antes de permitir una compra.
 *
 * <p>Las entidades lo mapean con {@code @Enumerated(EnumType.STRING)}, nunca {@code ORDINAL},
 * para que agregar un estado nuevo no corrompa las filas ya guardadas.
 *
 * <p>Es el <b>dato</b> del estado, no su comportamiento: que operaciones admite cada estado lo
 * deciden las clases del patron State en {@code internal.datos} (PAS-35). Transiciones validas:
 * {@code BORRADOR -> PUBLICADO}, {@code BORRADOR -> CANCELADO} y {@code PUBLICADO -> CANCELADO}.
 */
public enum EstadoEvento {

    /** Recien creado. No es visible en la cartelera publica y todavia se puede editar. */
    BORRADOR,

    /** Visible en la cartelera y habilitado para la venta de entradas. */
    PUBLICADO,

    /** Dado de baja. No admite ventas. */
    CANCELADO
}
