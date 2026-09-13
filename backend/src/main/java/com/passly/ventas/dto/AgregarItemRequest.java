package com.passly.ventas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Agrega una linea al carrito: cuantas entradas de que tipo. */
public record AgregarItemRequest(

        @NotNull(message = "el id del tipo de entrada es obligatorio")
        Long idTipoEntrada,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad tiene que ser mayor a cero")
        Integer cantidad
) {
}
