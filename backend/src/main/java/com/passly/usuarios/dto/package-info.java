/**
 * Objetos de transferencia del componente: la unica forma en que los datos de Usuarios cruzan
 * su frontera.
 *
 * <p><b>Por que hace falta esta anotacion:</b> en Spring Modulith los modulos son <i>cerrados</i>
 * por defecto — solo los tipos del paquete base del modulo son visibles desde afuera, y
 * <b>todo sub-paquete se considera interno</b>, este o no marcado como {@code internal}. Sin el
 * {@code @NamedInterface}, {@code com.passly.usuarios.dto} seria interno y el build fallaria el
 * dia que {@code ServicioDeVentas} importe {@code UsuarioDTO}.
 *
 * <p>Se declara desde el primer dia justamente para no descubrirlo mas adelante. A partir de
 * aca otro modulo puede declarar su dependencia como {@code "usuarios :: dto"}.
 */
@org.springframework.modulith.NamedInterface("dto")
package com.passly.usuarios.dto;
