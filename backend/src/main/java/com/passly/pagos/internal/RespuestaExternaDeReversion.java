package com.passly.pagos.internal;

import java.time.OffsetDateTime;

/**
 * Forma exacta en la que responde la pasarela externa (mockeada) a una reversión. Ver
 * {@link RespuestaExternaDeCobro} para la explicación completa del contrato.
 */
public record RespuestaExternaDeReversion(
        String reversalId,
        String status,
        OffsetDateTime processedAt
) {
}
