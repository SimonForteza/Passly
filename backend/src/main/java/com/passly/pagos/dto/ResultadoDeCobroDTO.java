package com.passly.pagos.dto;

import com.passly.pagos.EstadoDePago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Vista de salida de un cobro procesado: lo que el componente expone al mundo.
 *
 * <p>Es deliberadamente distinto de la entidad {@code Pago} interna — mismo criterio que
 * {@code eventos.dto.EventoDTO} con {@code Evento} — para que la forma de la API no quede atada
 * a decisiones de mapeo ORM.
 *
 * <p>Solo existe si el cobro fue {@link EstadoDePago#APROBADO}: un rechazo se comunica como
 * {@link com.passly.pagos.PagoRechazadoException}, no como un resultado con estado RECHAZADO, asi
 * que quien llama no necesita acordarse de chequear el campo {@code estado} para notar un
 * problema.
 */
public record ResultadoDeCobroDTO(
        Long idPago,
        String idTransaccionExterna,
        EstadoDePago estado,
        BigDecimal monto,
        String moneda,
        OffsetDateTime fechaProcesamiento
) {
}
