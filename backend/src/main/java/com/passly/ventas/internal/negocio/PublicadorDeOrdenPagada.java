package com.passly.ventas.internal.negocio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * No se envia JMS dentro del @Transactional: un rollback no debe anunciar una venta.
 * AFTER_COMMIT no es atomico con el broker: si cae, falta outbox para recuperar el envio.
 * No se propaga la falla: VentaServiceImpl interpretaria la excepcion como una compra fallida
 * y devolveria un pago cuya orden ya fue confirmada. No existe garantia exactly-once de transporte.
 */
@Component
class PublicadorDeOrdenPagada {
    private static final Logger log = LoggerFactory.getLogger(PublicadorDeOrdenPagada.class);
    private final JmsTemplate jms;
    private final JsonMapper mapper;

    PublicadorDeOrdenPagada(JmsTemplate jms, JsonMapper mapper) {
        this.jms = jms;
        this.mapper = mapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(OrdenPagada evento) {
        try {
            String json = mapper.writeValueAsString(evento);
            // TextMessage sin nombres de clases Java en headers: desacople entre componentes.
            jms.send("orden.pagada", session -> session.createTextMessage(json));
        } catch (RuntimeException ex) {
            log.error("Orden {} confirmada pero NO enviada a orden.pagada; requiere conciliacion manual (sin outbox)",
                    evento.idOrden(), ex);
        }
    }
}
