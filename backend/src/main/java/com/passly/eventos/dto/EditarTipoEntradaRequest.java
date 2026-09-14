package com.passly.eventos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud de edicion de un tipo de entrada ya existente (PAS-19): nombre, precio y cupo total.
 *
 * <p>Solo valida mientras el evento esta en {@code BORRADOR} — publicado, bajar el cupo o cambiar
 * el precio afectaria a quien ya compro o ya vio la cartelera; para sumar cupo con el evento
 * publicado esta {@code AmpliarCupoRequest}, una accion de negocio aparte, no una escritura de
 * campo. Bajar {@code cupoTotal} por debajo de lo ya vendido ({@code cupoTotal - cupoDisponible})
 * es invalido: esa regla vive en {@code TipoEntrada.editar}, no aca.
 */
public record EditarTipoEntradaRequest(

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
