package com.passly.accesos;

import com.passly.accesos.dto.ValidarEntradaRequest;
import com.passly.accesos.dto.ValidacionDTO.Resultado;
import com.passly.eventos.EventoService;
import com.passly.eventos.dto.*;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.dto.*;
import com.passly.tickets.TicketService;
import com.passly.tickets.dto.*;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "passly.facturacion.consumidor-activo=false")
class AccesosIntegrationTest {
    @Autowired AccesoService accesos;
    @Autowired TicketService tickets;
    @Autowired UsuarioService usuarios;
    @Autowired ProductoraService productoras;
    @Autowired EventoService eventos;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    UsuarioDTO validador, comprador, organizador, ajeno;
    EventoDTO evento, otroEvento;
    TicketDTO ticket;
    long orden;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        validador = usuario(Rol.VALIDADOR);
        comprador = usuario(Rol.COMPRADOR);
        organizador = usuario(Rol.ORGANIZADOR);
        ajeno = usuario(Rol.ORGANIZADOR);
        var p = productoras.crearProductora(new CrearProductoraRequest("Puerta " + UUID.randomUUID(), null, null, null), organizador.id());
        productoras.agregarMiembro(p.id(), new AgregarMiembroRequest(validador.email(), RolEnProductora.VALIDADOR), organizador.id());
        var otra = productoras.crearProductora(new CrearProductoraRequest("Otra " + UUID.randomUUID(), null, null, null), ajeno.id());
        evento = evento(p.id(), organizador.id());
        otroEvento = evento(otra.id(), ajeno.id());
        orden = UUID.randomUUID().getMostSignificantBits() >>> 1;
        tickets.emitirTickets(new EmitirTicketsRequest(orden, comprador.id(), List.of(
                new LineaDeEmisionDTO(evento.id(), evento.tiposEntrada().getFirst().id(), "General", 1))));
        ticket = tickets.listarTicketsDeOrden(orden, comprador.id()).getFirst();
    }

    @Test
    void validoYSegundoUsoConservanPrimerMomentoYAuditanAmbosIntentos() throws Exception {
        mvc.perform(post("/api/accesos/validaciones").with(user(validador.id().toString()).roles("VALIDADOR"))
                .contentType("application/json").content(body(evento.id(), ticket.contenidoQr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("VALIDO"))
                .andExpect(jsonPath("$.ticket.usadoEn").isNotEmpty());
        var primero = tickets.listarTicketsDeOrden(orden, comprador.id()).getFirst();
        var segundo = accesos.validarEntrada(new ValidarEntradaRequest(evento.id(), ticket.contenidoQr()), validador.id());
        assertThat(segundo.resultado()).isEqualTo(Resultado.YA_USADO);
        assertThat(segundo.ticket().usadoEn()).isEqualTo(primero.usadoEn());
        assertThat(jdbc.queryForObject("select count(*) from accesos.validacion where ticket_id=?", Long.class, ticket.id())).isEqualTo(2);
    }

    @Test
    void adulteradoODesconocidoEsInvalidoSinConsumir() throws Exception {
        for (String contenido : List.of(ticket.contenidoQr() + "x", "PASSLY:v1:no-existe", "")) {
            mvc.perform(post("/api/accesos/validaciones").with(user(validador.id().toString()).roles("VALIDADOR"))
                    .contentType("application/json").content(body(evento.id(), contenido)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("INVALIDO"));
        }
        assertThat(tickets.listarTicketsDeOrden(orden, comprador.id()).getFirst().usadoEn()).isNull();
        assertThat(jdbc.queryForObject("select count(*) from accesos.validacion where evento_id=?", Long.class, evento.id())).isEqualTo(3);
    }

    @Test
    void otroEventoNoConsumeNiExponeDatosDelTicket() {
        var resultado = accesos.validarEntrada(new ValidarEntradaRequest(otroEvento.id(), ticket.contenidoQr()), ajeno.id());
        assertThat(resultado.resultado()).isEqualTo(Resultado.OTRO_EVENTO);
        assertThat(resultado.ticket()).isNull();
        assertThat(accesos.validarEntrada(new ValidarEntradaRequest(evento.id(), ticket.contenidoQr()), validador.id()).resultado())
                .isEqualTo(Resultado.VALIDO);
    }

    @Test
    void rolYProductoraSeVerificanAntesDeConsumir() throws Exception {
        String body = body(evento.id(), ticket.contenidoQr());
        mvc.perform(post("/api/accesos/validaciones").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/accesos/validaciones").with(user(comprador.id().toString()).roles("COMPRADOR"))
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/accesos/validaciones").with(user(ajeno.id().toString()).roles("ORGANIZADOR"))
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/accesos/validaciones").with(user(validador.id().toString()).roles("VALIDADOR"))
                .contentType("application/json").content(body(otroEvento.id(), ticket.contenidoQr()))).andExpect(status().isForbidden());
        assertThat(tickets.listarTicketsDeOrden(orden, comprador.id()).getFirst().usadoEn()).isNull();
    }

    @Test
    void dosValidadoresConcurrentesSoloPermitenUnIngreso() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Resultado> validar = () -> {
                inicio.await();
                return accesos.validarEntrada(new ValidarEntradaRequest(evento.id(), ticket.contenidoQr()), validador.id()).resultado();
            };
            var a = pool.submit(validar);
            var b = pool.submit(validar);
            inicio.countDown();
            assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(Resultado.VALIDO, Resultado.YA_USADO);
        }
        assertThat(jdbc.queryForObject("select count(*) from accesos.validacion where ticket_id=?", Long.class, ticket.id())).isEqualTo(2);
    }

    @Test
    void solicitudIncompletaEs400YOrganizadorPropioPuedeValidar() throws Exception {
        mvc.perform(post("/api/accesos/validaciones").with(user(validador.id().toString()).roles("VALIDADOR"))
                .contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
        mvc.perform(post("/api/accesos/validaciones").with(user(organizador.id().toString()).roles("ORGANIZADOR"))
                .contentType("application/json").content(body(evento.id(), ticket.contenidoQr())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("VALIDO"));
    }

    private UsuarioDTO usuario(Rol rol) {
        return usuarios.registrarUsuario(new CrearUsuarioRequest(UUID.randomUUID() + "@test.com", "Prueba " + rol, "password123", rol));
    }
    private EventoDTO evento(Long productora, Long organizador) {
        var e = eventos.crearEvento(new CrearEventoRequest(productora, "Evento prueba", "Ingreso", OffsetDateTime.now().plusDays(2),
                "Puerta", List.of(new CrearTipoEntradaRequest("General", BigDecimal.TEN, 10))), organizador);
        return eventos.publicarEvento(e.id(), organizador);
    }
    private String body(Long evento, String contenido) {
        return "{\"idEvento\":" + evento + ",\"contenidoQr\":\"" + contenido + "\"}";
    }
}
