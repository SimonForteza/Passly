package com.passly.productoras.dto;

import com.passly.productoras.RolEnProductora;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Incorporacion de un usuario al padron de una productora.
 *
 * <p>A diferencia del creador de la productora, aca el {@code email} <b>si</b> es un dato
 * legitimo del request: no es quien actua, es a quien se invita. Quien actua sigue viajando aparte,
 * y tiene que ser DUENIO para que la operacion se acepte.
 *
 * <p>Se identifica por email y no por id porque el id generado no es descubrible desde la app: es
 * el mismo criterio que ya usa {@code CrearUsuarioRequest}.
 */
public record AgregarMiembroRequest(

        @NotBlank(message = "el email del usuario a incorporar es obligatorio")
        @Email(message = "el email no tiene un formato valido")
        String email,

        @NotNull(message = "el rol en la productora es obligatorio")
        RolEnProductora rolEnProductora
) {
}
