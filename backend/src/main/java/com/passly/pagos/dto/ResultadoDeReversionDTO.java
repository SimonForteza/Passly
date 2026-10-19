package com.passly.pagos.dto;

import com.passly.pagos.EstadoDePago;

import java.time.OffsetDateTime;

/**
 * Vista de salida de una reversión procesada, simétrica a {@link ResultadoDeCobroDTO}: informa
 * el resultado en vez de solo confirmar que la operación terminó.
 */
public record ResultadoDeReversionDTO(
        Long idPago,
        String idTransaccionExterna,
        String idTransaccionReversion,
        EstadoDePago estado,
        OffsetDateTime fechaReversion
) {
}
