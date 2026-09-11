package com.passly.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blindaje de la operacion sensible: un usuario <b>autenticado</b> con rol equivocado
 * (COMPRADOR) no puede crear eventos. El body es valido a proposito, para que la respuesta sea el
 * <b>403</b> del {@code @PreAuthorize("hasRole('ORGANIZADOR')")} y no un 400 de validacion: la
 * validacion de {@code @RequestBody} corre antes que el chequeo de autorizacion.
 *
 * <p>El {@code MockMvc} se arma a mano con {@code springSecurity()} en vez de inyectarlo con
 * {@code @AutoConfigureMockMvc}: ese configurer instala el filtro que puentea el usuario de
 * {@code @WithMockUser} al {@code SecurityContext} de la request. Sin el, la request llegaria
 * anonima y el filter chain cortaria con 401 antes de evaluar el {@code @PreAuthorize} (que es lo
 * que da el 403).
 */
@SpringBootTest
class AutorizacionPorRolTest {

    private static final String EVENTO_VALIDO = """
            {
              "nombre": "Festival de prueba",
              "fechaHora": "2030-01-01T20:00:00Z",
              "lugar": "Parque Centenario, CABA",
              "tiposEntrada": [ { "nombre": "General", "precio": 15000.00, "cupoTotal": 200 } ]
            }
            """;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "COMPRADOR")
    void unCompradorAutenticadoNoPuedeCrearEvento() throws Exception {
        mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVENTO_VALIDO))
                .andExpect(status().isForbidden());
    }
}
