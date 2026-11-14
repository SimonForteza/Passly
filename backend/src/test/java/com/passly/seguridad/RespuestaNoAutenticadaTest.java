package com.passly.seguridad;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El 401 del filtro de seguridad, contra un Tomcat real (PAS-33).
 *
 * <p>No alcanza con MockMvc: su respuesta simulada escribe en UTF-8 por defecto, mientras que
 * Tomcat usa ISO-8859-1 si nadie fija la codificacion. Como el entry point escribe el
 * {@code ProblemDetail} a mano, sin fijarla un cliente real recibia "Sesi�n" en lugar de
 * "Sesión". Solo un servidor de verdad reproduce ese bug.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RespuestaNoAutenticadaTest {

    @LocalServerPort
    private int puerto;

    @Test
    void elMensajeDel401LlegaEnUtf8() throws Exception {
        HttpResponse<byte[]> respuesta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/usuarios/me")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
        assertThat(new String(respuesta.body(), StandardCharsets.UTF_8))
                .contains("Sesión ausente, inválida o vencida");
    }
}
