package com.passly.tickets.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Solicitud completa de emision de tickets para una orden confirmada. */
public record EmitirTicketsRequest(
        @NotNull Long idOrden,
        @NotNull Long idComprador,
        @NotEmpty List<@Valid LineaDeEmisionDTO> lineas
) {
    public EmitirTicketsRequest {
        lineas = lineas == null ? null : List.copyOf(lineas);
    }
}
