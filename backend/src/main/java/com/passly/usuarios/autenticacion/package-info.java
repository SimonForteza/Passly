/**
 * Contrato de autenticacion del componente: la <b>unica</b> puerta por la que la credencial
 * hasheada cruza la frontera de Usuarios.
 *
 * <p><b>Por que es un named interface aparte de {@code dto}.</b> El paquete {@code dto} expone la
 * vista publica de un usuario, que a proposito <i>no</i> incluye el hash. La autenticacion, en
 * cambio, necesita el hash para que el {@code DaoAuthenticationProvider} de Spring Security haga
 * el {@code matches}. Separar el contrato deja explicito que exponer la credencial es una
 * concesion acotada a un unico consumidor —el modulo {@code seguridad}— y no parte del contrato
 * general que usa el resto del sistema. Un modulo declara esta dependencia como
 * {@code "usuarios :: autenticacion"}.
 *
 * <p>Como todo sub-paquete en Spring Modulith es interno por defecto, sin este
 * {@code @NamedInterface} el build fallaria el dia que {@code seguridad} importe
 * {@link com.passly.usuarios.autenticacion.CredencialDTO}.
 */
@org.springframework.modulith.NamedInterface("autenticacion")
package com.passly.usuarios.autenticacion;
