package com.passly.ventas.internal.negocio;

import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicadorDeOrdenPagadaTest {
    @Test
    void fallaDelBrokerNoPropagaUnaExcepcionQueDevuelvaElPagoConfirmado() {
        JmsTemplate jms = mock(JmsTemplate.class);
        doThrow(new IllegalStateException("broker caido")).when(jms).send(eq("orden.pagada"), any(MessageCreator.class));
        var publicador = new PublicadorDeOrdenPagada(jms, JsonMapper.builder().findAndAddModules().build());
        assertThatCode(() -> publicador.enviar(new OrdenPagada(1L, 2L, BigDecimal.TEN, OffsetDateTime.now())))
                .doesNotThrowAnyException();
        verify(jms).send(eq("orden.pagada"), any(MessageCreator.class));
    }
}
