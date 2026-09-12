package com.passly.productoras.dto;

import com.passly.productoras.RolEnProductora;

/**
 * Una linea del padron de una productora.
 *
 * <p>{@code email} y {@code nombre} no salen de la entidad {@code Miembro}, que solo guarda el
 * {@code usuarioId}: los resuelve el servicio preguntandole a {@code UsuarioService}. Es la
 * composicion en memoria que reemplaza al join entre esquemas que CLAUDE.md 4.8 prohibe.
 */
public record MiembroDTO(
        Long idUsuario,
        String email,
        String nombre,
        RolEnProductora rolEnProductora
) {
}
