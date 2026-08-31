package com.passly.eventos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Un tipo de entrada dentro de la solicitud de creacion de un evento.
 *
 * <p>No tiene endpoint propio: se crea siempre anidado en el evento. Es consecuencia de que
 * {@code Evento} sea la raiz del agregado — un tipo de entrada no tiene ciclo de vida propio.
 *
 * <p>{@code BigDecimal} y no {@code double}: es dinero, y el punto flotante binario no puede
 * representar exactamente valores decimales.
 */
public record CrearTipoEntradaRequest(

        @NotBlank(message = "el nombre del tipo de entrada es obligatorio")
        @Size(max = 80, message = "el nombre no puede superar los 80 caracteres")
        String nombre,

        @NotNull(message = "el precio es obligatorio")
        @DecimalMin(value = "0.0", message = "el precio no puede ser negativo")
        BigDecimal precio,

        @NotNull(message = "el cupo total es obligatorio")
        @Positive(message = "el cupo total debe ser mayor a cero")
        Integer cupoTotal
) {
}
