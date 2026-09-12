/**
 * Objetos de transferencia del componente: la unica forma en que los datos de Ventas cruzan su
 * frontera.
 *
 * <p>Spring Modulith cierra los modulos por defecto: sin este {@code @NamedInterface}, este
 * sub-paquete seria interno y el build fallaria el dia que otro componente importe un DTO de
 * Ventas.
 */
@org.springframework.modulith.NamedInterface("dto")
package com.passly.ventas.dto;
