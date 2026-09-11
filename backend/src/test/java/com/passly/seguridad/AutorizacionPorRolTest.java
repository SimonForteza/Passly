package com.passly.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blindaje de la operacion sensible: un usuario <b>autenticado</b> con rol equivocado
 * (COMPRADOR) no puede crear eventos. El body es valido a proposito, para que la respuesta sea el
 * <b>403</b> del {@code @PreAuthorize("hasRole('ORGANIZADOR')")} y no un 400 de validacion: la
 * validacion de {@code @RequestBody} corre antes que el chequeo de autorizacion.
 */
@SpringBootTest
@AutoConfigureMockMvc
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
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "COMPRADOR")
    void unCompradorAutenticadoNoPuedeCrearEvento() throws Exception {
        mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVENTO_VALIDO))
                .andExpect(status().isForbidden());
    }
}
