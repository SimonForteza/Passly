package com.passly.pagos.internal;

import java.time.OffsetDateTime;

/**
 * Forma exacta en la que responde la pasarela externa (mockeada). Ver
 * {@link SolicitudExternaDeCobro} para la explicacion completa de por que este contrato es
 * distinto del propio de Pagos y por que vive publico en {@code internal}.
 *
 * <p>{@code status} llega como {@code "APPROVED"} o {@code "DECLINED"} — el vocabulario de la
 * pasarela, no el de Passly. Traducirlo a {@link com.passly.pagos.EstadoDePago} es tarea de
 * {@code PagoServiceImpl}.
 */
public record RespuestaExternaDeCobro(
        String transactionId,
        String status,
        OffsetDateTime processedAt
) {
}
