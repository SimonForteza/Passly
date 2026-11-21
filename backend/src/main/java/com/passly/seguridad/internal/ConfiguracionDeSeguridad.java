package com.passly.seguridad.internal;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de seguridad de Passly: autenticacion con <b>JWT</b> (header
 * {@code Authorization: Bearer}), sesion <b>STATELESS</b> y method security habilitado para los
 * {@code @PreAuthorize} de los controllers.
 *
 * <p><b>De HTTP Basic a JWT (PAS-33).</b> Hasta la Obligatoria 1 la API usaba Basic: cada request
 * mandaba email y contraseña, y el backend corria BCrypt en cada una. La consigna de la 2.a entrega
 * exige JWT. Ahora la contraseña viaja una sola vez, a {@code POST /api/auth/login}
 * ({@link AutenticacionController}), que devuelve un token firmado; el resto de las requests
 * presenta ese token y el resource server de Spring Security lo valida (firma HS256, emisor y
 * vencimiento, ver {@link ConfiguracionDeJwt}). Lo que no cambio: el reparto rol / membresia, los
 * {@code @PreAuthorize}, y que {@code Authentication#getName()} sea el id del usuario
 * ({@link ConvertidorDeJwtAAutenticacion}).
 *
 * <p><b>Reparto de responsabilidades:</b> este filter chain decide solo entre <i>anonimo</i> y
 * <i>autenticado</i> (grano grueso). <b>Que rol</b> puede cada operacion sensible se declara con
 * {@code @PreAuthorize} en los controllers (grano fino), que es lo que pide el requerimiento.
 *
 * <p><b>STATELESS y el carrito de ServicioDeVentas (PAS-8), la tension que hay que saber
 * explicar.</b> Esta politica solo le dice a <i>Spring Security</i> que no cree ni lea
 * {@code HttpSession} para guardar el {@code SecurityContext}: cada request se re-autentica con
 * su propio token. No le dice nada al <i>servlet container</i>, que sigue creando una
 * sesion en cuanto algo la pide -- y eso es exactamente lo que hace el proxy de un bean
 * {@code @SessionScope} como {@code CarritoDeCompra}. La autenticacion sigue siendo stateless; el
 * carrito, no. Consecuencia real, no cosmetica: con nada que asocie el {@code JSESSIONID} al
 * principal, alguien que reutilizara la cookie de otro junto con sus propias credenciales veria
 * su carrito. Por eso el carrito valida al dueño en cada operacion
 * ({@code CarritoDeOtroCompradorException}) en vez de confiar en la sesion a secas.
 *
 * <p><b>CORS (PAS-14).</b> La app web (Vite, otro origen) llama a esta API con
 * {@code credentials: 'include'} porque necesita la cookie de sesion del carrito ademas del
 * header {@code Authorization}. Sin una {@link CorsConfigurationSource} explicita el navegador bloquea esas
 * llamadas antes de que lleguen al filtro de arriba -- no es una migracion de arquitectura,
 * es la contraparte obligatoria de exponer la API a un cliente que corre en otro puerto. El
 * origen permitido sale de {@code passly.web.origen} (mismo patron de externalizacion que
 * {@code passly.pagos.pasarela.base-url}), con default al puerto de Vite en desarrollo.
 *
 * <p><b>El 401, en {@code ProblemDetail} y con {@code WWW-Authenticate: Bearer}.</b> Con Basic el
 * desafio se omitia a proposito, porque {@code WWW-Authenticate: Basic} hacia que el navegador
 * mostrara su popup nativo de usuario y contraseña antes que la pantalla de login de la SPA. El
 * desafio {@code Bearer} (RFC 6750) no dispara ningun popup, asi que
 * {@link #puntoDeEntradaNoAutenticado} lo vuelve a mandar, con el cuerpo en {@code ProblemDetail}
 * como el resto de los errores de la API. Aplica tanto a la falta de token en una ruta protegida
 * como a un token invalido o vencido.
 *
 * <p><b>Un token invalido da 401 incluso en una ruta publica</b> (la cartelera, por ejemplo): el
 * filtro de Bearer valida todo token que reciba, antes de mirar si la ruta lo exige. Es el
 * comportamiento por defecto de Spring Security y es el correcto: un token adulterado no se ignora
 * en silencio. El cliente que tenga un token vencido tiene que descartarlo (PAS-34).
 */
@Configuration
@EnableMethodSecurity
class ConfiguracionDeSeguridad {

    @Bean
    SecurityFilterChain filtros(
            HttpSecurity http,
            CorsConfigurationSource fuenteDeConfiguracionCors,
            AuthenticationEntryPoint puntoDeEntradaNoAutenticado,
            ConvertidorDeJwtAAutenticacion convertidorDeJwt
    ) throws Exception {
        http
                // Ver Javadoc de la clase: sin esto el navegador bloquea las llamadas de la app
                // web (otro origen) antes de que lleguen a authorizeHttpRequests.
                .cors(cors -> cors.configurationSource(fuenteDeConfiguracionCors))
                // Desde PAS-8 SI hay una cookie de sesion (el carrito de Ventas es @SessionScope),
                // pero CSRF sigue apagado con sentido: la identidad la sigue dando el header
                // Authorization (Bearer), que un formulario cross-site no puede setear. La cookie habilita
                // "session riding" sobre el carrito, no falsificar quien opera -- por eso el
                // carrito valida a su dueño en cada operacion (ver Javadoc de la clase).
                .csrf(csrf -> csrf.disable())
                // Spring Security no crea ni lee HttpSession para el SecurityContext: cada
                // request se autentica de nuevo con su token. No impide que la app pida sesion
                // por otro motivo (ver Javadoc de la clase).
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cartelera publica: cualquiera ve los eventos publicados y un evento por id.
                        .requestMatchers(HttpMethod.GET, "/api/invitaciones").permitAll()
                        .requestMatchers("/api/ventas/invitados/transferencia").authenticated()
                        .requestMatchers("/api/ventas/invitados/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/eventos", "/api/eventos/*").permitAll()
                        // Portada (PAS-47): la cartelera la pide con un <img>, que no manda token.
                        // El borrador igual da 404 al publico: lo decide el servicio.
                        .requestMatchers(HttpMethod.GET, "/api/eventos/*/portada").permitAll()
                        // Cartelera de productoras: publico, es lo que elige el comprador.
                        .requestMatchers(HttpMethod.GET, "/api/productoras", "/api/productoras/*").permitAll()
                        // Alta publica: el POST es anonimo; que solo pueda crear COMPRADOR lo impone
                        // el @PreAuthorize del controller, no este filtro.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // Login (PAS-33): canjea email + contraseña por un JWT. Publico por definicion.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Healthcheck sin credenciales, para probes y demo.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // El mock de la pasarela de pago (PAS-7/PAS-29, com.passly.pagos.internal.pasarelamock)
                        // simula un proveedor externo al que PagoServiceImpl le habla por HTTP real.
                        // Un proveedor real tendria su propio esquema de autenticacion, nunca las
                        // credenciales de Passly -- y PagoServiceImpl no manda ningun header
                        // Authorization en esa llamada saliente. Sin este permitAll, anyRequest()
                        // exigiria credenciales que nadie va a mandar y todo cobro terminaria en 401.
                        .requestMatchers(HttpMethod.POST, "/mock/pasarela-pago/**").permitAll()
                        // Todo lo demas exige, al menos, estar autenticado.
                        .anyRequest().authenticated())
                // Resource server: valida el JWT del header Authorization y arma la Authentication
                // con el id como nombre y el rol vigente de Usuarios (ConvertidorDeJwtAAutenticacion).
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeJwt))
                        .authenticationEntryPoint(puntoDeEntradaNoAutenticado))
                // Sin token en una ruta protegida: el mismo 401 que un token invalido.
                .exceptionHandling(errores -> errores.authenticationEntryPoint(puntoDeEntradaNoAutenticado));

        return http.build();
    }

    /** Ver Javadoc de la clase: 401 en {@code ProblemDetail}, con el desafio {@code Bearer}. */
    @Bean
    AuthenticationEntryPoint puntoDeEntradaNoAutenticado(ObjectMapper mapper) {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            // Sin esto el writer sale en ISO-8859-1 (default del servlet) y las tildes se rompen.
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ProblemDetail detalle = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "Sesión ausente, inválida o vencida. Iniciá sesión de nuevo.");
            mapper.writeValue(response.getWriter(), detalle);
        };
    }

    /**
     * Origen permitido para CORS: por defecto el puerto de Vite en desarrollo. Externalizado
     * por property (ver Javadoc de la clase) para no hardcodear el origen de produccion.
     */
    @Bean
    CorsConfigurationSource fuenteDeConfiguracionCors(
            @Value("${passly.web.origen}") String origenWeb
    ) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(origenWeb));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Invitacion-Token", "X-Orden-Token", "Idempotency-Key"));
        // El carrito depende de la cookie de sesion ademas del header Authorization (ver Javadoc).
        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
