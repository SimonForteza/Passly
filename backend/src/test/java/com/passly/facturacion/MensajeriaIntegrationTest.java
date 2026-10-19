package com.passly.facturacion;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=18083",
    "passly.pagos.pasarela.base-url=http://localhost:18083",
    "spring.artemis.broker-url=tcp://127.0.0.1:61618",
    "spring.artemis.user=", "spring.artemis.password=",
    "passly.facturacion.consumidor-activo=true"
})
@Import(MensajeriaIntegrationTest.Broker.class)
class MensajeriaIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class Broker {
        @Bean(initMethod = "start", destroyMethod = "stop")
        ActiveMQServer brokerDePrueba() throws Exception {
            var config = new ConfigurationImpl().setPersistenceEnabled(false).setSecurityEnabled(false)
                    .addAcceptorConfiguration("test", "tcp://127.0.0.1:61618");
            config.addQueueConfiguration(QueueConfiguration.of("orden.pagada").setRoutingType(RoutingType.ANYCAST));
            config.addQueueConfiguration(QueueConfiguration.of("DLQ").setRoutingType(RoutingType.ANYCAST));
            config.addAddressSetting("#", new AddressSettings().setMaxDeliveryAttempts(3)
                    .setRedeliveryDelay(100).setDeadLetterAddress(org.apache.activemq.artemis.api.core.SimpleString.of("DLQ")));
            return ActiveMQServers.newActiveMQServer(config);
        }
    }
    @Autowired JmsTemplate jms;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.jms.config.JmsListenerEndpointRegistry listeners;
    @Autowired ActiveMQServer broker;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired com.passly.usuarios.UsuarioService usuarios;
    @Autowired com.passly.productoras.ProductoraService productoras;
    @Autowired com.passly.eventos.EventoService eventos;

    @Test
    void compraConfirmadaEsperaEnElBrokerHastaReactivarConsumidor() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        var comprador = usuarios.registrarUsuario(new com.passly.usuarios.dto.CrearUsuarioRequest(
                "compra-" + sufijo + "@test.com", "Comprador", "password123", com.passly.usuarios.Rol.COMPRADOR));
        var organizador = usuarios.registrarUsuario(new com.passly.usuarios.dto.CrearUsuarioRequest(
                "organiza-" + sufijo + "@test.com", "Organizador", "password123", com.passly.usuarios.Rol.ORGANIZADOR));
        var productora = productoras.crearProductora(new com.passly.productoras.dto.CrearProductoraRequest(
                "Mensajeria " + sufijo, null, null, null), organizador.id());
        var evento = eventos.crearEvento(new com.passly.eventos.dto.CrearEventoRequest(productora.id(),
                "Prueba JMS", "Compra real", java.time.OffsetDateTime.now().plusDays(3), "Puerta 1",
                java.util.List.of(new com.passly.eventos.dto.CrearTipoEntradaRequest("General", java.math.BigDecimal.TEN, 10))), organizador.id());
        eventos.publicarEvento(evento.id(), organizador.id());
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var actor = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user(comprador.id().toString()).roles("COMPRADOR");
        var sesion = new org.springframework.mock.web.MockHttpSession();
        var listener = listeners.getListenerContainer("facturacion-orden-pagada");
        listener.stop();
        try {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ventas/carrito/items")
                    .with(actor).session(sesion).contentType("application/json")
                    .content("{\"idTipoEntrada\":" + evento.tiposEntrada().getFirst().id() + ",\"cantidad\":1}"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            String respuesta = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ventas/ordenes")
                    .with(actor).session(sesion).contentType("application/json")
                    .content("{\"tarjeta\":{\"numero\":\"4242424242424242\",\"titular\":\"Comprador\",\"vencimiento\":\"12/30\",\"codigoSeguridad\":\"123\"}}"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            long orden = tools.jackson.databind.json.JsonMapper.builder().build().readTree(respuesta).get("id").asLong();
            assertThat(jdbc.queryForObject("select count(*) from facturacion.factura where orden_id=?", Long.class, orden)).isZero();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(broker.locateQueue("orden.pagada").getMessageCount()).isEqualTo(1));
            listener.start();
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(jdbc.queryForObject("select count(*) from facturacion.factura where orden_id=?", Long.class, orden)).isEqualTo(1));
        } finally {
            listener.start();
        }
    }


    @Test
    void configuracionDockerDeclaraColaDurableYDlq() throws Exception {
        var config = new org.apache.activemq.artemis.core.config.impl.FileConfiguration();
        new org.apache.activemq.artemis.core.config.FileDeploymentManager(
                java.nio.file.Path.of("../infra/artemis/broker.xml").toUri().toString())
                .addDeployable(config).readConfiguration();
        assertThat(config.getAddressConfigurations().stream().flatMap(a -> a.getQueueConfigurations().stream()).toList()).anySatisfy(q -> {
            assertThat(q.getName().toString()).isEqualTo("orden.pagada");
            assertThat(q.isDurable()).isTrue();
        });
        assertThat(config.getAddressSettings().get("#").getMaxDeliveryAttempts()).isEqualTo(3);
    }

    @Test
    void mensajeDuplicadoGeneraUnaSolaFactura() {
        long orden = Math.abs(UUID.randomUUID().getMostSignificantBits() >>> 1);
        String mensaje = "{\"idOrden\":" + orden + ",\"idComprador\":1,\"total\":10,\"cobradaEn\":\"2026-10-18T20:00:00Z\"}";
        jms.convertAndSend("orden.pagada", mensaje);
        jms.convertAndSend("orden.pagada", mensaje);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("select count(*) from facturacion.factura where orden_id=?", Long.class, orden)).isEqualTo(1));
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("select count(*) from facturacion.factura where orden_id=? and estado='PENDIENTE_AFIP'", Long.class, orden)).isEqualTo(1));
    }

    @Test
    void mensajeInvalidoReintentaYTerminaEnDlq() {
        String invalido = "{\"idOrden\":-1}";
        jms.convertAndSend("orden.pagada", invalido);
        jms.setReceiveTimeout(500);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jms.receiveAndConvert("DLQ")).isEqualTo(invalido));
    }
}
