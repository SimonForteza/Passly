package com.passly.productoras.dto;

import com.passly.productoras.RolEnProductora;
import jakarta.validation.constraints.NotNull;

/**
 * Incorporacion de un usuario al padron de una productora.
 *
 * <p>A diferencia del creador de la productora, aca el {@code idUsuario} <b>si</b> es un dato
 * legitimo del request: no es quien actua, es a quien se invita. Quien actua sigue viajando aparte,
 * y tiene que ser DUENIO para que la operacion se acepte.
 */
public record AgregarMiembroRequest(

        @NotNull(message = "el id del usuario a incorporar es obligatorio")
        Long idUsuario,

        @NotNull(message = "el rol en la productora es obligatorio")
        RolEnProductora rolEnProductora
) {
}
