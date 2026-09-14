package com.passly.eventos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud de ampliacion de cupo de un tipo de entrada (PAS-19): suma {@code cantidad} tanto al
 * cupo total como al disponible.
 *
 * <p>Es una accion de negocio con su propia regla — admitida con el evento {@code PUBLICADO},
 * a diferencia de {@code EditarTipoEntradaRequest} — y por eso tiene su propio endpoint
 * ({@code POST .../ampliacion-de-cupo}) en vez de ser un caso mas del {@code PUT} del tipo de
 * entrada, igual que {@code POST .../publicacion} es una accion y no un {@code PATCH} de estado.
 */
public record AmpliarCupoRequest(

        @NotNull(message = "la cantidad a sumar es obligatoria")
        @Positive(message = "la cantidad a sumar debe ser mayor a cero")
        Integer cantidad
) {
}
