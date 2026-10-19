package com.passly.ventas;

import com.passly.eventos.EventoService;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.tickets.TicketService;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de integracion sobre la pila HTTP completa (controller + Spring Security +
 * transaccion real contra Postgres), igual patron que {@code AutorizacionPorRolTest}:
 * {@code MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())}.
 *
 * <p><b>Identidad dinamica, no {@code @WithMockUser}.</b> El id del comprador lo genera la base
 * en cada corrida (fixture por UUID, sin depender del perfil {@code demo}), asi que no puede ser
 * un valor fijo en una anotacion: se inyecta por request con
 * {@code user(String).roles(...)} de {@code spring-security-test}, que hace que
 * {@code Authentication#getName()} devuelva exactamente ese id, igual que en produccion.
 *
 * <p><b>La misma {@link MockHttpSession} en varias llamadas</b> es lo que hace que el bean
 * {@code @SessionScope} sea la <b>misma instancia</b> entre requests — sin esto, cada
 * {@code perform(...)} veria un carrito vacio nuevo. Es la version de test de lo que en la app
 * real hace la cookie {@code JSESSIONID}.
 *
 * <p><b>Nunca {@code @Transactional} en esta clase</b>: envolveria la compra bajo prueba en la
 * transaccion del test y el rollback de {@code ConfirmacionDeCompra} no se podria observar desde
 * afuera (todo se revertiria igual al final, incluso el caso exitoso).
 *
 * <p><b>{@code webEnvironment = DEFINED_PORT} (PAS-29), y no el {@code MOCK} por defecto.</b>
 * Confirmar una compra ahora cobra de verdad contra {@code PagoServiceImpl}, que le habla al
 * mock de la pasarela por {@code RestClient} -- HTTP real, no un dispatcher en memoria. Sin un
 * servidor real escuchando en el puerto configurado ({@code server.port: 8080}), esa llamada
 * saliente se encuentra con nadie del otro lado y todo cobro termina en
 * {@code PasarelaDePagoNoDisponibleException} (503), sin importar la tarjeta. El
 * {@code MockMvc} de este test sigue golpeando el {@code DispatcherServlet} directo (no hace
 * una llamada de red), asi que no compite por el mismo puerto.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18081",
                "passly.pagos.pasarela.base-url=http://localhost:18081",
                "passly.facturacion.consumidor-activo=false"})
class ConfirmarCompraTest {

    private static final String TARJETA_APROBADA = "4242424242424242";
    private static final String TARJETA_RECHAZADA = "4000000000000002";
    private static final String TARJETA_PASARELA_CAIDA = "4000000000000119";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoraService productoraService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private TicketService ticketService;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private org.springframework.jms.core.JmsTemplate jms;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.doNothing().when(jms).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(org.springframework.jms.core.MessageCreator.class));
        org.mockito.Mockito.clearInvocations(jms);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /**
     * El criterio de aceptacion central de PAS-8: un carrito de dos lineas donde la primera
     * tiene cupo de sobra y la segunda lo excede revierte <b>tambien</b> la primera -- porque
     * {@code ConfirmacionDeCompra.confirmar} es una sola transaccion declarativa -- y no deja
     * ninguna orden persistida. Es el mismo mecanismo que se demuestra en vivo con
     * {@code docs/http/08-ventas.http}, probado aca de punta a punta contra Postgres real.
     */
    @Test
    void elRollbackDevuelveElCupoYNoDejaOrden() throws Exception {
        UsuarioDTO comprador = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(5, 2);
        Long idConSobra = evento.tiposEntrada().get(0).id();
        Long idSinCupo = evento.tiposEntrada().get(1).id();

        MockHttpSession sesion = new MockHttpSession();
        var actuante = user(String.valueOf(comprador.id())).roles("COMPRADOR");

        agregarAlCarrito(sesion, actuante, idConSobra, 3);
        agregarAlCarrito(sesion, actuante, idSinCupo, 5);

        mockMvc.perform(confirmarCompra(actuante, sesion, TARJETA_APROBADA))
                .andExpect(status().isConflict());

        DisponibilidadDTO conSobra = eventoService.consultarDisponibilidad(idConSobra);
        DisponibilidadDTO sinCupo = eventoService.consultarDisponibilidad(idSinCupo);
        assertThat(conSobra.cupoDisponible()).isEqualTo(5);
        assertThat(sinCupo.cupoDisponible()).isEqualTo(2);

        org.mockito.Mockito.verify(jms, org.mockito.Mockito.never()).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(org.springframework.jms.core.MessageCreator.class));

        // El carrito sobrevive: no se vacio porque la compra no se confirmo.
        mockMvc.perform(get("/api/ventas/carrito").with(actuante).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    /** 402: la pasarela proceso el pedido y lo rechazo. El carrito sobrevive para reintentar. */
    @Test
    void confirmarCompraConTarjetaRechazadaDevuelve402() throws Exception {
        UsuarioDTO comprador = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(10, 10);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        MockHttpSession sesion = new MockHttpSession();
        var actuante = user(String.valueOf(comprador.id())).roles("COMPRADOR");
        agregarAlCarrito(sesion, actuante, idTipoEntrada, 2);

        mockMvc.perform(confirmarCompra(actuante, sesion, TARJETA_RECHAZADA))
                .andExpect(status().isPaymentRequired());

        DisponibilidadDTO disponibilidad = eventoService.consultarDisponibilidad(idTipoEntrada);
        assertThat(disponibilidad.cupoDisponible()).isEqualTo(10);

        mockMvc.perform(get("/api/ventas/carrito").with(actuante).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    /** 503: la pasarela no respondio. Problema tecnico externo, no un bug de Passly. */
    @Test
    void confirmarCompraConPasarelaCaidaDevuelve503() throws Exception {
        UsuarioDTO comprador = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(10, 10);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        MockHttpSession sesion = new MockHttpSession();
        var actuante = user(String.valueOf(comprador.id())).roles("COMPRADOR");
        agregarAlCarrito(sesion, actuante, idTipoEntrada, 2);

        mockMvc.perform(confirmarCompra(actuante, sesion, TARJETA_PASARELA_CAIDA))
                .andExpect(status().isServiceUnavailable());

        DisponibilidadDTO disponibilidad = eventoService.consultarDisponibilidad(idTipoEntrada);
        assertThat(disponibilidad.cupoDisponible()).isEqualTo(10);

        mockMvc.perform(get("/api/ventas/carrito").with(actuante).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void unCompradorNoVeLaOrdenDeOtro() throws Exception {
        UsuarioDTO compradorA = registrarComprador();
        UsuarioDTO compradorB = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(10, 10);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        MockHttpSession sesionA = new MockHttpSession();
        var actuanteA = user(String.valueOf(compradorA.id())).roles("COMPRADOR");
        agregarAlCarrito(sesionA, actuanteA, idTipoEntrada, 1);

        String respuesta = mockMvc.perform(confirmarCompra(actuanteA, sesionA, TARJETA_APROBADA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long idOrden = extraerId(respuesta);

        var actuanteB = user(String.valueOf(compradorB.id())).roles("COMPRADOR");
        mockMvc.perform(get("/api/ventas/ordenes/" + idOrden).with(actuanteB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/ventas/ordenes/" + idOrden).with(actuanteA))
                .andExpect(status().isOk());
    }

    @Test
    void confirmarCompraEmiteUnTicketPorCadaUnidad() throws Exception {
        UsuarioDTO comprador = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(10, 10);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();

        MockHttpSession sesion = new MockHttpSession();
        var actuante = user(String.valueOf(comprador.id())).roles("COMPRADOR");
        agregarAlCarrito(sesion, actuante, idTipoEntrada, 3);

        String respuesta = mockMvc.perform(confirmarCompra(actuante, sesion, TARJETA_APROBADA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long idOrden = extraerId(respuesta);

        org.mockito.Mockito.verify(jms).send(org.mockito.ArgumentMatchers.eq("orden.pagada"),
                org.mockito.ArgumentMatchers.any(org.springframework.jms.core.MessageCreator.class));
        assertThat(ticketService.listarTicketsDeOrden(idOrden, comprador.id()))
                .hasSize(3)
                .allSatisfy(ticket -> {
                    assertThat(ticket.idOrden()).isEqualTo(idOrden);
                    assertThat(ticket.qrBase64()).isNotBlank();
                });
    }

    /** Dos sesiones distintas del mismo comprador no comparten carrito: cada una es su propia instancia. */
    @Test
    void dosSesionesDistintasNoComparenElCarrito() throws Exception {
        UsuarioDTO comprador = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(10, 10);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();
        var actuante = user(String.valueOf(comprador.id())).roles("COMPRADOR");

        MockHttpSession sesionUno = new MockHttpSession();
        agregarAlCarrito(sesionUno, actuante, idTipoEntrada, 2);

        MockHttpSession sesionDos = new MockHttpSession();
        mockMvc.perform(get("/api/ventas/carrito").with(actuante).session(sesionDos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        // La primera sesion sigue con su linea, sin que la segunda la haya tocado.
        mockMvc.perform(get("/api/ventas/carrito").with(actuante).session(sesionUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    /** Abandonar el carrito invalida la sesion: el contenedor destruye el bean en el acto. */
    @Test
    void abandonarElCarritoLoDejaVacioParaLaSesionSiguiente() throws Exception {
        UsuarioDTO comprador = registrarComprador();
        EventoDTO evento = crearEventoConDosTiposDeEntrada(10, 10);
        Long idTipoEntrada = evento.tiposEntrada().get(0).id();
        var actuante = user(String.valueOf(comprador.id())).roles("COMPRADOR");

        MockHttpSession sesion = new MockHttpSession();
        agregarAlCarrito(sesion, actuante, idTipoEntrada, 1);

        mockMvc.perform(delete("/api/ventas/carrito").with(actuante).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        assertThat(sesion.isInvalid()).isTrue();
    }

    /**
     * Confirma la compra con una tarjeta dada. El numero de tarjeta decide el resultado contra
     * el mock de la pasarela (CLAUDE.md 9): {@code TARJETA_APROBADA} cobra, {@code
     * TARJETA_RECHAZADA} devuelve 402, {@code TARJETA_PASARELA_CAIDA} devuelve 503.
     */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder confirmarCompra(
            org.springframework.test.web.servlet.request.RequestPostProcessor actuante,
            MockHttpSession sesion,
            String numeroTarjeta
    ) {
        return post("/api/ventas/ordenes")
                .with(actuante)
                .session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tarjeta\":{\"numero\":\"" + numeroTarjeta + "\",\"titular\":\"Comprador de Prueba\","
                        + "\"vencimiento\":\"12/29\",\"codigoSeguridad\":\"123\"}}");
    }

    private void agregarAlCarrito(
            MockHttpSession sesion,
            org.springframework.test.web.servlet.request.RequestPostProcessor actuante,
            Long idTipoEntrada,
            int cantidad
    ) throws Exception {
        mockMvc.perform(post("/api/ventas/carrito/items")
                        .with(actuante)
                        .session(sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTipoEntrada\":" + idTipoEntrada + ",\"cantidad\":" + cantidad + "}"))
                .andExpect(status().isOk());
    }

    private static Long extraerId(String jsonOrden) {
        // Sin libreria de parseo aca a proposito: es un solo campo al principio del JSON,
        // "id":<numero>, y evita sumar una dependencia solo para un test.
        String marca = "\"id\":";
        int inicio = jsonOrden.indexOf(marca) + marca.length();
        int fin = jsonOrden.indexOf(',', inicio);
        return Long.valueOf(jsonOrden.substring(inicio, fin));
    }

    private UsuarioDTO registrarComprador() {
        String sufijo = UUID.randomUUID().toString();
        return usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "comprador-" + sufijo + "@test.passly",
                "Comprador de Prueba",
                "passly1234",
                Rol.COMPRADOR));
    }

    private EventoDTO crearEventoConDosTiposDeEntrada(int cupoUno, int cupoDos) {
        String sufijo = UUID.randomUUID().toString();

        UsuarioDTO organizador = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador-" + sufijo + "@test.passly",
                "Organizador de Prueba",
                "passly1234",
                Rol.ORGANIZADOR));

        Long idProductora = productoraService.crearProductora(
                new CrearProductoraRequest("Productora " + sufijo, null, null, null),
                organizador.id()
        ).id();

        List<CrearTipoEntradaRequest> tiposEntrada = List.of(
                new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), cupoUno),
                new CrearTipoEntradaRequest("VIP", new BigDecimal("2000.00"), cupoDos));

        return eventoService.crearEvento(new CrearEventoRequest(
                idProductora,
                "Evento de Prueba " + sufijo,
                "Fixture de ConfirmarCompraTest",
                OffsetDateTime.now().plusMonths(1),
                "Lugar de Prueba",
                tiposEntrada
        ), organizador.id());
    }
}
