package com.passly.usuarios;

/**
 * Rol de un usuario en el sistema.
 *
 * <p>Vive en la raiz del modulo y no en {@code internal.datos} porque es <b>parte del
 * contrato</b>: la seguridad por rol (Spring Security + {@code @PreAuthorize}, PAS-6) y
 * {@code ServicioDeVentas} necesitan conocer el rol de quien opera sin poder ver la entidad.
 *
 * <p>Las entidades lo mapean con {@code @Enumerated(EnumType.STRING)}, nunca {@code ORDINAL},
 * para que agregar un rol nuevo no corrompa las filas ya guardadas.
 */
public enum Rol {

    /** Compra entradas. Es el rol con el que un usuario se auto-registra. */
    COMPRADOR,

    /** Crea y administra sus eventos, y ve sus reportes. */
    ORGANIZADOR,

    /** Escanea y valida el QR en la puerta del evento. */
    VALIDADOR,

    /** Administra la plataforma. */
    ADMIN
}
