package com.passly.eventos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Solicitud de edicion de los datos propios de un evento (PAS-19): nombre, descripcion,
 * fecha/hora y lugar. No lleva {@code idProductora} — un evento no cambia de dueño,
 * {@code Evento.productoraId} es {@code updatable = false} (CLAUDE.md, ver {@code Evento}) — ni
 * {@code tiposEntrada}, que tienen sus propios endpoints porque su edicion admite reglas distintas
 * segun el estado del evento (ver {@code EditarTipoEntradaRequest} y {@code AmpliarCupoRequest}).
 *
 * <p>Solo valida mientras el evento esta en {@code BORRADOR}: publicado, cambiar estos datos
 * afectaria una cartelera que ya vio (o compro) alguien. Esa regla es de negocio y vive en
 * {@code Evento.editarDatos}, no aca — igual que {@code CrearEventoRequest}, esto solo valida forma.
 */
public record EditarEventoRequest(

        @NotBlank(message = "el nombre del evento es obligatorio")
        @Size(max = 150, message = "el nombre no puede superar los 150 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "la fecha y hora son obligatorias")
        @Future(message = "la fecha del evento debe ser futura")
        OffsetDateTime fechaHora,

        @NotBlank(message = "el lugar es obligatorio")
        @Size(max = 200, message = "el lugar no puede superar los 200 caracteres")
        String lugar
) {
}
