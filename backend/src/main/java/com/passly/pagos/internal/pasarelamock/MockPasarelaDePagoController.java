package com.passly.pagos.internal.pasarelamock;

import com.passly.pagos.internal.RespuestaExternaDeCobro;
import com.passly.pagos.internal.SolicitudExternaDeCobro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Standin de la pasarela de pago externa: simula lo que responderia un proveedor real (Mercado
 * Pago, Stripe) sobre HTTP real, no un stub en memoria.
 *
 * <p><b>Por que un controller propio y no WireMock/Testcontainers:</b> cero dependencias nuevas,
 * corre dentro del mismo proceso que ya se levanta para la demo, y {@code PagoServiceImpl} le
 * habla por {@link org.springframework.web.client.RestClient} exactamente como le hablaria a un
 * proveedor real — la unica diferencia entre esto y produccion es el valor de
 * {@code passly.pagos.pasarela.base-url} (ver {@code internal.ConfiguracionDePagos}). Es el mismo
 * criterio que CLAUDE.md aplica a AFIP: mockear el endpoint en vez de pelear con un proveedor
 * real o con certificados de homologacion.
 *
 * <p><b>Convenciones de tarjetas de prueba tomadas de pasarelas reales (Stripe):</b> el numero
 * {@code 4000000000000002} simula un rechazo y {@code 4000000000000119} simula que la pasarela no
 * responde (503) — para poder demostrar los dos caminos de error sin depender del azar. Cualquier
 * otro numero se aprueba.
 *
 * <p>Vive en su propio sub-paquete de {@code internal}, distinto de {@code internal.web}: no es
 * la API de Passly, es la simulacion de la API de otro. Que este dentro de {@code pagos} y no en
 * un modulo aparte es deliberado — no le agrega superficie de contacto al grafo de Spring
 * Modulith, porque {@code PagoServiceImpl} le habla por red, no por un import de Java.
 */
@RestController
@RequestMapping("/mock/pasarela-pago")
class MockPasarelaDePagoController {

    private static final String TARJETA_RECHAZADA = "4000000000000002";
    private static final String TARJETA_PASARELA_CAIDA = "4000000000000119";

    @PostMapping("/cobros")
    ResponseEntity<RespuestaExternaDeCobro> cobrar(@RequestBody SolicitudExternaDeCobro solicitud) {
        if (TARJETA_PASARELA_CAIDA.equals(solicitud.cardNumber())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        String estado = TARJETA_RECHAZADA.equals(solicitud.cardNumber()) ? "DECLINED" : "APPROVED";
        RespuestaExternaDeCobro respuesta = new RespuestaExternaDeCobro(
                "ext-" + UUID.randomUUID(), estado, OffsetDateTime.now());

        return ResponseEntity.ok(respuesta);
    }
}
