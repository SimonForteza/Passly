/**
 * Objetos de transferencia del componente: la unica forma en que los datos de Productoras cruzan
 * su frontera.
 *
 * <p><b>Por que hace falta esta anotacion:</b> en Spring Modulith los modulos son <i>cerrados</i>
 * por defecto — solo los tipos del paquete base del modulo son visibles desde afuera, y
 * <b>todo sub-paquete se considera interno</b>, este o no marcado como {@code internal}. Sin el
 * {@code @NamedInterface}, {@code com.passly.productoras.dto} seria interno y el build fallaria
 * cuando {@code ServicioDeEventos} importe {@code ProductoraDTO} para atribuir sus eventos.
 *
 * <p>A partir de aca otro modulo puede declarar su dependencia como
 * {@code "productoras :: dto"}.
 */
@org.springframework.modulith.NamedInterface("dto")
package com.passly.productoras.dto;
