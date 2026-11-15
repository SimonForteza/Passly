package com.passly.eventos.internal.web;

import com.jayway.jsonpath.JsonPath;
import com.passly.eventos.EventoService;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.EventoDTO;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/eventos/{id}/cancelacion} (PAS-35), de punta a punta: JWT real, filter chain,
 * {@code @PreAuthorize}, servicio, patron State y {@code ManejadorDeErrores}. La matriz completa
 * estado x operacion se prueba sin Spring en {@code MatrizDeEstadosDeEventoTest}; aca se fija que
 * el 409 del State llega al cliente como {@code ProblemDetail}.
 */
@SpringBootTest
class CancelacionDeEventoTest {

    private static final String CLAVE = "passly1234";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoraService productoraService;

    @Autowired
    private EventoService eventoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void cancelarDosVecesDa200YDespues409ConProblemDetail() throws Exception {
        Fixture fixture = crearEventoPublicado();
        RequestPostProcessor token = conToken(fixture.organizador.email());

        mockMvc.perform(post("/api/eventos/{id}/cancelacion", fixture.evento.id()).with(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        mockMvc.perform(post("/api/eventos/{id}/cancelacion", fixture.evento.id()).with(token))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void publicarUnEventoCanceladoDa409() throws Exception {
        Fixture fixture = crearEventoPublicado();
        eventoService.cancelarEvento(fixture.evento.id(), fixture.organizador.id());

        mockMvc.perform(post("/api/eventos/{id}/publicacion", fixture.evento.id())
                        .with(conToken(fixture.organizador.email())))
                .andExpect(status().isConflict());
    }

    @Test
    void unOrganizadorDeOtraProductoraNoPuedeCancelar() throws Exception {
        Fixture fixture = crearEventoPublicado();
        Fixture otra = crearEventoPublicado();

        mockMvc.perform(post("/api/eventos/{id}/cancelacion", fixture.evento.id())
                        .with(conToken(otra.organizador.email())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unEventoCanceladoDejaDeTenerDisponibilidadYSaleDeLaCartelera() {
        Fixture fixture = crearEventoPublicado();
        Long idTipoEntrada = fixture.evento.tiposEntrada().get(0).id();
        assertThat(eventoService.consultarDisponibilidad(idTipoEntrada).hayDisponibilidad()).isTrue();

        eventoService.cancelarEvento(fixture.evento.id(), fixture.organizador.id());

        assertThat(eventoService.consultarDisponibilidad(idTipoEntrada).hayDisponibilidad()).isFalse();
        assertThat(eventoService.listarEventosPublicados())
                .extracting(EventoDTO::id)
                .doesNotContain(fixture.evento.id());
    }

    private record Fixture(EventoDTO evento, UsuarioDTO organizador) {
    }

    /** Inicia sesion de verdad contra {@code POST /api/auth/login} y manda el JWT como Bearer. */
    private RequestPostProcessor conToken(String email) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = JsonPath.read(cuerpo, "$.token");
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private Fixture crearEventoPublicado() {
        String sufijo = UUID.randomUUID().toString();

        UsuarioDTO organizador = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador-" + sufijo + "@test.passly",
                "Organizador de Prueba",
                CLAVE,
                Rol.ORGANIZADOR));

        Long idProductora = productoraService.crearProductora(
                new CrearProductoraRequest("Productora " + sufijo, null, null, null),
                organizador.id()
        ).id();

        EventoDTO borrador = eventoService.crearEvento(new CrearEventoRequest(
                idProductora,
                "Evento de Prueba " + sufijo,
                "Fixture de CancelacionDeEventoTest",
                OffsetDateTime.now().plusMonths(1),
                "Lugar de Prueba",
                List.of(new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), 10))
        ), organizador.id());

        return new Fixture(eventoService.publicarEvento(borrador.id(), organizador.id()), organizador);
    }
}
