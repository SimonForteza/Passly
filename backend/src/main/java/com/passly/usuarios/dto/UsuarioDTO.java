package com.passly.usuarios.dto;

import com.passly.usuarios.Rol;

/**
 * Vista de salida de un usuario: lo que el componente expone al mundo.
 *
 * <p><b>No incluye el hash de la contrasena</b>, a proposito. La entidad {@code Usuario} guarda
 * {@code passwordHash}, pero la credencial no forma parte de esta vista publica. Es la aplicacion
 * concreta de la regla "las entidades JPA nunca salen del componente" (CLAUDE.md 4.3) al dato mas
 * sensible del sistema.
 *
 * <p>El hash cruza la frontera <b>solo</b> por el contrato de autenticacion
 * ({@code usuarios :: autenticacion}, {@link com.passly.usuarios.autenticacion.CredencialDTO}),
 * que consume el modulo {@code seguridad} para el login. Ese es el unico camino; este DTO general
 * sigue sin exponerlo.
 */
public record UsuarioDTO(
        Long id,
        String email,
        String nombre,
        Rol rol
) {
}
