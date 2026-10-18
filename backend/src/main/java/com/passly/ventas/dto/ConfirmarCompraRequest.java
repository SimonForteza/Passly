package com.passly.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Body de {@code POST /api/ventas/ordenes}: los datos de tarjeta con los que se cobra. */
public record ConfirmarCompraRequest(

        @NotNull(message = "los datos de la tarjeta son obligatorios")
        @Valid
        DatosDeTarjetaDTO tarjeta
) {
}
