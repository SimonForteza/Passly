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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portada de evento (PAS-47) de punta a punta: JWT real, filter chain, {@code @PreAuthorize},
 * servicio, patron State, validacion por firma del archivo, ETag y {@code ManejadorDeErrores}.
 */
@SpringBootTest
class PortadaDeEventoTest {

    private static final String CLAVE = "passly1234";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 9, 9};

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
    void subirLaPortadaDeUnPublicadoLaSirveAlPublicoConSuTipoYConUrlVersionada() throws Exception {
        Fixture f = crearEvento(true);

        mockMvc.perform(subir(f.evento.id(), JPEG).with(conToken(f.organizador.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portadaUrl")
                        .value(org.hamcrest.Matchers.startsWith("/api/eventos/" + f.evento.id() + "/portada?v=")));

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")));
    }

    @Test
    void conElMismoEtagResponde304SinBytesYAlCambiarLaPortadaCambiaElEtag() throws Exception {
        Fixture f = crearEvento(true);
        eventoService.cambiarPortada(f.evento.id(), JPEG, f.organizador.id());

        String etag = mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()))
                .andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()).header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(content().bytes(new byte[0]));

        Thread.sleep(5);
        eventoService.cambiarPortada(f.evento.id(), PNG, f.organizador.id());

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()).header("If-None-Match", etag))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void unArchivoQueNoEsImagenDa400ConProblemDetailAunqueDigaSerJpeg() throws Exception {
        Fixture f = crearEvento(true);
        byte[] html = "<html>no soy una foto</html>".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(subir(f.evento.id(), html).with(conToken(f.organizador.email())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("JPG, PNG o WebP")));
    }

    @Test
    void enUnEventoCanceladoDa409YNoGuardaLaImagen() throws Exception {
        Fixture f = crearEvento(true);
        eventoService.cancelarEvento(f.evento.id(), f.organizador.id());

        mockMvc.perform(subir(f.evento.id(), JPEG).with(conToken(f.organizador.email())))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void otraProductoraDa403YAnonimoDa401() throws Exception {
        Fixture f = crearEvento(true);
        Fixture otra = crearEvento(true);

        mockMvc.perform(subir(f.evento.id(), JPEG).with(conToken(otra.organizador.email())))
                .andExpect(status().isForbidden());
        mockMvc.perform(subir(f.evento.id(), JPEG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laPortadaDeUnBorradorDa404AlPublicoPeroLaVeQuienLoGestiona() throws Exception {
        Fixture f = crearEvento(false);
        eventoService.cambiarPortada(f.evento.id(), JPEG, f.organizador.id());

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()).with(conToken(f.organizador.email())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    void quitarLaPortadaVuelveAlRespaldo() throws Exception {
        Fixture f = crearEvento(true);
        eventoService.cambiarPortada(f.evento.id(), JPEG, f.organizador.id());

        mockMvc.perform(delete("/api/eventos/{id}/portada", f.evento.id()).with(conToken(f.organizador.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portadaUrl").doesNotExist());

        mockMvc.perform(get("/api/eventos/{id}/portada", f.evento.id()))
                .andExpect(status().isNotFound());
    }

    private static MockMultipartHttpServletRequestBuilder subir(Long idEvento, byte[] contenido) {
        MockMultipartHttpServletRequestBuilder builder =
                multipart(HttpMethod.PUT, "/api/eventos/{id}/portada", idEvento);
        builder.file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", contenido));
        return builder;
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

    private Fixture crearEvento(boolean publicado) {
        String sufijo = UUID.randomUUID().toString();
        UsuarioDTO organizador = usuarioService.registrarUsuario(new CrearUsuarioRequest(
                "organizador-" + sufijo + "@test.passly", "Organizador de Prueba", CLAVE, Rol.ORGANIZADOR));
        Long idProductora = productoraService.crearProductora(
                new CrearProductoraRequest("Productora " + sufijo, null, null, null), organizador.id()).id();
        EventoDTO evento = eventoService.crearEvento(new CrearEventoRequest(
                idProductora,
                "Evento de Prueba " + sufijo,
                "Fixture de PortadaDeEventoTest",
                OffsetDateTime.now().plusMonths(1),
                "Lugar de Prueba",
                List.of(new CrearTipoEntradaRequest("General", new BigDecimal("1000.00"), 10))
        ), organizador.id());
        if (publicado) {
            evento = eventoService.publicarEvento(evento.id(), organizador.id());
        }
        return new Fixture(evento, organizador);
    }
}
