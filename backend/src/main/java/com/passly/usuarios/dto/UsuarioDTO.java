package com.passly.usuarios.dto;

import com.passly.usuarios.Rol;

/**
 * Vista de salida de un usuario: lo que el componente expone al mundo.
 *
 * <p><b>No incluye el hash de la contrasena</b>, a proposito. La entidad {@code Usuario} guarda
 * {@code passwordHash}, pero la credencial no forma parte de ninguna respuesta: nunca cruza la
 * frontera del componente. Es la aplicacion concreta de la regla "las entidades JPA nunca salen
 * del componente" (CLAUDE.md 4.3) al dato mas sensible del sistema.
 */
public record UsuarioDTO(
        Long id,
        String email,
        String nombre,
        Rol rol
) {
}
