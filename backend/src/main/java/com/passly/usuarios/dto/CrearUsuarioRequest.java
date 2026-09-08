package com.passly.usuarios.dto;

import com.passly.usuarios.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de alta de un usuario.
 *
 * <p>Las restricciones de aca son de <b>forma</b> y las verifica la capa de presentacion con
 * {@code @Valid}. La regla de <b>negocio</b> (que el email no este ya registrado) vive en la
 * capa de negocio, no aca.
 *
 * <p><b>El {@code password} es lo unico sensible que entra por el sistema.</b> Llega en texto
 * plano solo en este request, se hashea con BCrypt en la capa de negocio y no se persiste ni se
 * loguea nunca. El tope de 72 caracteres es el limite de bytes que BCrypt considera: mas alla de
 * eso los caracteres se ignorarian en silencio, y es mejor rechazarlo explicitamente.
 */
public record CrearUsuarioRequest(

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato valido")
        @Size(max = 254, message = "el email no puede superar los 254 caracteres")
        String email,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "la contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "la contrasena debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "el rol es obligatorio")
        Rol rol
) {
}
