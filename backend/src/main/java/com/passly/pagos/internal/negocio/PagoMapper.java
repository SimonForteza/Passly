package com.passly.pagos.internal.negocio;

import com.passly.pagos.dto.ResultadoDeCobroDTO;
import com.passly.pagos.internal.datos.Pago;
import org.springframework.stereotype.Component;

/**
 * Entidad JPA -&gt; DTO. Es el unico lugar del componente donde {@link Pago} y
 * {@link ResultadoDeCobroDTO} se tocan (mismo criterio que
 * {@code eventos.internal.negocio.EventoMapper}, CLAUDE.md 4.3): la regla "las entidades JPA
 * nunca salen del componente" es auditable porque el mapeo esta concentrado en un solo archivo.
 */
@Component
class PagoMapper {

    ResultadoDeCobroDTO aDTO(Pago pago) {
        return new ResultadoDeCobroDTO(
                pago.getId(),
                pago.getIdTransaccionExterna(),
                pago.getEstado(),
                pago.getMonto(),
                pago.getMoneda(),
                pago.getFechaProcesamiento()
        );
    }
}
