package com.passly.tickets.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Snapshot de una linea de orden necesario para emitir tickets sin consultar Eventos. */
public record LineaDeEmisionDTO(
        @NotNull Long idEvento,
        @NotNull Long idTipoEntrada,
        @NotBlank String nombreTipoEntrada,
        @NotNull @Min(1) Integer cantidad
) {
}
