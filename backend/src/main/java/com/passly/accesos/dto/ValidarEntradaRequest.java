package com.passly.accesos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ValidarEntradaRequest(
        @NotNull @Positive Long idEvento,
        @NotNull @Size(max = 512) String contenidoQr) {}
