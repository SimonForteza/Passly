package com.passly.pagos.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wiring del cliente HTTP hacia la pasarela de pago externa. Vive directo en {@code internal}, no
 * en {@code internal.negocio}: es infraestructura de acceso a un tercero, no una regla de
 * dominio — el mismo criterio que {@code seguridad.internal.ConfiguracionDeSeguridad}.
 *
 * <p><b>La URL base es una propiedad, no una constante.</b> Hoy {@code passly.pagos.pasarela.base-url}
 * apunta al mock que corre en este mismo proceso ({@code internal.pasarelamock}); pasar a un
 * proveedor real el dia de mañana es cambiar esa propiedad, nada de codigo — el mismo patron de
 * externalizacion que las variables de entorno de la base de datos (CLAUDE.md 4.8).
 *
 * <p><b>Los timeouts son deliberados.</b> Sin ellos, una pasarela colgada (no caida: colgada, sin
 * responder) bloquearia el hilo que la llama indefinidamente. Un externo lento no puede convertirse
 * en un problema de disponibilidad de Passly — es la misma idea de CLAUDE.md 2 sobre no dejar que
 * lo externo controle cuanto tarda el sistema propio.
 */
@Configuration
class ConfiguracionDePagos {

    @Bean
    RestClient pasarelaDePago(
            RestClient.Builder builder,
            @Value("${passly.pagos.pasarela.base-url}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
