package com.passly.usuarios.autenticacion;

import com.passly.usuarios.Rol;

/**
 * Credencial de un usuario para autenticacion: lo minimo que el modulo {@code seguridad} necesita
 * para construir un {@code UserDetails} y delegar la verificacion en Spring Security.
 *
 * <p><b>Es el unico DTO que lleva el {@code passwordHash} fuera del componente</b>, y a proposito:
 * el {@link com.passly.usuarios.dto.UsuarioDTO} general no lo incluye. El hash viaja hasheado con
 * BCrypt, nunca en texto plano, y su unico consumidor previsto es el proveedor de autenticacion.
 *
 * @param email        identidad de login
 * @param passwordHash hash BCrypt de la contrasena (nunca el texto plano)
 * @param rol          rol del usuario, que {@code seguridad} mapea a una authority {@code ROLE_*}
 */
public record CredencialDTO(
        String email,
        String passwordHash,
        Rol rol
) {
}
