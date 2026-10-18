package com.passly.eventos.internal.web;

import com.passly.eventos.EventoNoEncontradoException;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/eventos/{id}}, el destino del link compartible (PAS-25): un BORRADOR solo lo ve
 * quien gestiona la productora dueña; para todos los demas responde 404, no 403 (CLAUDE.md 4.11).
 *
 * <p>Las requests autenticadas pasan por HTTP Basic de verdad (no {@code @WithMockUser}), porque lo
 * que se prueba depende de que el principal lleve el id numerico como username, tal como lo arma
 * {@code seguridad}. Mismo fixture por contratos publicos que {@code EdicionDeEventoTest}.
 *
 * <p>Vive en {@code internal.web} para poder llamar al controller directo con un
 * {@link AnonymousAuthenticationToken}: segun como Spring resuelva el parametro, el anonimo puede
 * llegar como {@code null} o como ese token, y los dos tienen que contar como anonimo.
 */
@SpringBootTest
class DetalleDeEventoTest {

    private static final String CLAVE = "passly1234";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoraService productoraService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private EventoController eventoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unAnonimoNoVeUnBorrador() throws Exception {
        Fixture fixture = crearBorrador();

        mockMvc.perform(get("/api/eventos/{id}", fixture.evento.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unAnonimoConTokenAnonimoTampocoVeUnBorrador() throws Exception {
        Fixture fixture = crearBorrador();

        mockMvc.perform(get("/api/eventos/{id}", fixture.evento.id()).with(anonymous()))
                .andExpect(status().isNotFound());
    }

    @Test
    void elControllerTrataElAnonymousAuthenticationTokenComoAnonimo() {
        Fixture fixture = crearBorrador();
        AnonymousAuthenticationToken anonimo = new AnonymousAuthenticationToken(
                "clave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        // Sin el chequeo, getName() = "anonymousUser" haria fallar el parseo del id con
        // NumberFormatException en lugar de responder como anonimo.
        assertThatThrownBy(() -> eventoController.consultarEvento(fixture.evento.id(), anonimo))
                .isInstanceOf(EventoNoEncontradoException.class);

        eventoService.publicarEvento(fixture.evento.id(), fixture.organizador.id());
        assertThat(eventoController.consultarEvento(fixture.evento.id(), anonimo).id())
                .isEqualTo(fixture.evento.id());
    }

    @Test
    void elOrganizadorDeLaProductoraVeSuBorrador() throws Exception {
        Fixture fixture = crearBorrador();

        mockMvc.perform(get("/api/eventos/{id}", fixture.evento.id())
                        .with(httpBasic(fixture.organizador.email(), CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void unOrganizadorDeOtraProductoraNoVeElBorrador() throws Exception {
        Fixture fixture = crearBorrador();
        Fixture otra = crearBorrador();

        mockMvc.perform(get("/api/eventos/{id}", fixture.evento.id())
                        .with(httpBasic(otra.organizador.email(), CLAVE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unAnonimoVeUnEventoPublicado() throws Exception {
        Fixture fixture = crearBorrador();
        eventoService.publicarEvento(fixture.evento.id(), fixture.organizador.id());

        mockMvc.perform(get("/api/eventos/{id}", fixture.evento.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
    }

    @Test
    void unOrganizadorDeOtraProductoraVeUnEventoPublicado() throws Exception {
        Fixture fixture = crearBorrador();
        Fixture otra = crearBorrador();
        eventoService.publicarEvento(fixture.evento.id(), fixture.organizador.id());

        mockMvc.perform(get("/api/eventos/{id}", fixture.evento.id())
                        .with(httpBasic(otra.organizador.email(), CLAVE)))
                .andExpect(status().isOk());
    }

    private record Fixture(EventoDTO evento, UsuarioDTO organizador) {
    }

    private Fixture crearBorrador() {
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

        EventoDTO evento = eventoService.crearEvento(new CrearEventoRequest(
                idProductora,
                "Evento de Prueba " + sufijo,
                "Fixture de DetalleDeEventoTest",
                OffsetDateTime.now().plusMonths(1),
                "Lugar de Prueba",
                List.of(new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), 10))
        ), organizador.id());

        return new Fixture(evento, organizador);
    }
}
