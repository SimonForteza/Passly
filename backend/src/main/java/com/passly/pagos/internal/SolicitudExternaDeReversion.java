package com.passly.pagos.internal;

/**
 * Forma exacta que espera el endpoint de reversión de la pasarela externa (mockeada). Ver
 * {@link SolicitudExternaDeCobro} para la explicación completa de por qué este contrato es
 * distinto del propio de Pagos y por qué vive público en {@code internal}.
 */
public record SolicitudExternaDeReversion(String merchantReference) {
}
