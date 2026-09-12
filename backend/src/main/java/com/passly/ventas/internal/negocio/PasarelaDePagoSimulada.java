package com.passly.ventas.internal.negocio;

import com.passly.ventas.PagoRechazadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Adapter de {@link PasarelaDePago} que reemplaza a {@code ServicioDePagos} (PAS-7, todavia sin
 * implementar) mientras tanto. Aprueba cualquier cobro salvo que supere {@link #LIMITE} — un
 * limite de transaccion es una regla real de cualquier pasarela, y da un camino deterministico
 * y explicable para ejercitar {@code PagoRechazadoException} sin inventar un fallo artificial.
 */
@Component
class PasarelaDePagoSimulada implements PasarelaDePago {

    private static final Logger log = LoggerFactory.getLogger(PasarelaDePagoSimulada.class);

    /** Limite de transaccion de la pasarela simulada. */
    static final BigDecimal LIMITE = new BigDecimal("1000000.00");

    @Override
    public ComprobanteDeCobroDTO cobrar(BigDecimal importe) {
        if (importe.compareTo(LIMITE) > 0) {
            throw new PagoRechazadoException(
                    "el importe ($" + importe + ") supera el limite de la pasarela ($" + LIMITE + ")");
        }
        String idComprobante = "SIM-" + UUID.randomUUID();
        log.info("Cobro simulado aprobado: {} por ${}", idComprobante, importe);
        return new ComprobanteDeCobroDTO(idComprobante, importe, OffsetDateTime.now());
    }

    @Override
    public void revertir(String idComprobante) {
        log.info("Cobro simulado revertido: {}", idComprobante);
    }
}
