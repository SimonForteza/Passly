package com.passly.pagos.internal;

import java.math.BigDecimal;

/**
 * Forma exacta que espera el endpoint REST de la pasarela externa (mockeada en
 * {@code internal.pasarelamock}).
 *
 * <p>Deliberadamente <b>no es igual</b> a {@code pagos.dto.SolicitudDeCobroDTO}: una pasarela
 * real define su propio contrato, con sus propios nombres de campo (en ingles, al estilo de
 * Stripe/Mercado Pago), y traducir entre las dos formas es exactamente el trabajo del Adapter.
 * Que ambos contratos coincidieran por casualidad no demostraria nada; que sean distintos y
 * {@code PagoServiceImpl} los reconcilie es la evidencia del patron.
 *
 * <p><b>Por que es publica y vive en {@code internal}, no en {@code internal.negocio}:</b> es el
 * contrato compartido entre las dos puntas de la simulacion — quien lo arma
 * ({@code internal.negocio.PagoServiceImpl}) y quien lo recibe
 * ({@code internal.pasarelamock.MockPasarelaDePagoController}) — y la visibilidad de paquete de
 * Java <b>no es jerarquica</b>: {@code internal.negocio} e {@code internal.pasarelamock} son
 * paquetes distintos y no se ven entre si (la misma razon por la que {@code eventos.Evento} y su
 * repositorio son publicos, CLAUDE.md 4.3). Que sea publica no la vuelve parte del contrato del
 * modulo: Spring Modulith sigue impidiendo que otro componente la importe.
 */
public record SolicitudExternaDeCobro(
        BigDecimal amount,
        String currency,
        String cardNumber,
        String cardHolder,
        String cardExpiry,
        String cardCvv,
        String merchantReference
) {
}
