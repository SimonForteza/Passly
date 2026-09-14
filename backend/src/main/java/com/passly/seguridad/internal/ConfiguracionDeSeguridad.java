package com.passly.seguridad.internal;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de seguridad de Passly: autenticacion <b>HTTP Basic</b>, sesion
 * <b>STATELESS</b> y method security habilitado para los {@code @PreAuthorize} de los controllers.
 *
 * <p><b>Por que HTTP Basic y no JWT:</b> la Obligatoria 1 pide autorizacion por rol declarativa,
 * no gestion de sesiones distribuida. Basic sobre una API stateless alcanza; JWT seria
 * complejidad sin beneficio para el alcance actual.
 *
 * <p><b>Reparto de responsabilidades:</b> este filter chain decide solo entre <i>anonimo</i> y
 * <i>autenticado</i> (grano grueso). <b>Que rol</b> puede cada operacion sensible se declara con
 * {@code @PreAuthorize} en los controllers (grano fino), que es lo que pide el requerimiento.
 *
 * <p><b>STATELESS y el carrito de ServicioDeVentas (PAS-8), la tension que hay que saber
 * explicar.</b> Esta politica solo le dice a <i>Spring Security</i> que no cree ni lea
 * {@code HttpSession} para guardar el {@code SecurityContext}: cada request se re-autentica con
 * su propio header Basic. No le dice nada al <i>servlet container</i>, que sigue creando una
 * sesion en cuanto algo la pide -- y eso es exactamente lo que hace el proxy de un bean
 * {@code @SessionScope} como {@code CarritoDeCompra}. La autenticacion sigue siendo stateless; el
 * carrito, no. Consecuencia real, no cosmetica: con nada que asocie el {@code JSESSIONID} al
 * principal, alguien que reutilizara la cookie de otro junto con sus propias credenciales veria
 * su carrito. Por eso el carrito valida al dueño en cada operacion
 * ({@code CarritoDeOtroCompradorException}) en vez de confiar en la sesion a secas.
 *
 * <p><b>CORS (PAS-14).</b> La app web (Vite, otro origen) llama a esta API con
 * {@code credentials: 'include'} porque necesita la cookie de sesion del carrito ademas del
 * header Basic. Sin una {@link CorsConfigurationSource} explicita el navegador bloquea esas
 * llamadas antes de que lleguen al filtro de arriba -- no es una migracion de arquitectura,
 * es la contraparte obligatoria de exponer la API a un cliente que corre en otro puerto. El
 * origen permitido sale de {@code passly.web.origen} (mismo patron de externalizacion que
 * {@code passly.pagos.pasarela.base-url}), con default al puerto de Vite en desarrollo.
 *
 * <p><b>Sin desafio {@code WWW-Authenticate} en el 401 (app web, PAS-15/16).</b> El
 * {@link org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint} por
 * defecto de {@code httpBasic()} manda {@code WWW-Authenticate: Basic} en cada 401 -- correcto
 * para un cliente HTTP puro (curl, Postman), pero el navegador intercepta ese header incluso en
 * una llamada {@code fetch} y muestra su propio popup nativo de usuario/contrasena <i>antes</i> de
 * que el JS de la SPA vea la respuesta. Como la app web ya resuelve el 401 con su propia pantalla
 * de login (mensaje "Email o contraseña inválidos."), {@link #puntoDeEntradaNoAutenticado} devuelve
 * el mismo 401 sin ese header, para que el navegador no interfiera.
 */
@Configuration
@EnableMethodSecurity
class ConfiguracionDeSeguridad {

    @Bean
    SecurityFilterChain filtros(
            HttpSecurity http,
            CorsConfigurationSource fuenteDeConfiguracionCors,
            AuthenticationEntryPoint puntoDeEntradaNoAutenticado
    ) throws Exception {
        http
                // Ver Javadoc de la clase: sin esto el navegador bloquea las llamadas de la app
                // web (otro origen) antes de que lleguen a authorizeHttpRequests.
                .cors(cors -> cors.configurationSource(fuenteDeConfiguracionCors))
                // Desde PAS-8 SI hay una cookie de sesion (el carrito de Ventas es @SessionScope),
                // pero CSRF sigue apagado con sentido: la identidad la sigue dando el header
                // Authorization, que un formulario cross-site no puede setear. La cookie habilita
                // "session riding" sobre el carrito, no falsificar quien opera -- por eso el
                // carrito valida a su dueño en cada operacion (ver Javadoc de la clase).
                .csrf(csrf -> csrf.disable())
                // Spring Security no crea ni lee HttpSession para el SecurityContext: cada
                // request se autentica de nuevo con su Basic. No impide que la app pida sesion
                // por otro motivo (ver Javadoc de la clase).
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cartelera publica: cualquiera ve los eventos publicados y un evento por id.
                        .requestMatchers(HttpMethod.GET, "/api/eventos", "/api/eventos/*").permitAll()
                        // Cartelera de productoras: publico, es lo que elige el comprador.
                        .requestMatchers(HttpMethod.GET, "/api/productoras", "/api/productoras/*").permitAll()
                        // Alta publica: el POST es anonimo; que solo pueda crear COMPRADOR lo impone
                        // el @PreAuthorize del controller, no este filtro.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // Healthcheck sin credenciales, para probes y demo.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Todo lo demas exige, al menos, estar autenticado.
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(puntoDeEntradaNoAutenticado));

        return http.build();
    }

    /** Ver Javadoc de la clase: mismo 401 que el entry point por defecto, sin {@code WWW-Authenticate}. */
    @Bean
    AuthenticationEntryPoint puntoDeEntradaNoAutenticado(ObjectMapper mapper) {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            ProblemDetail detalle = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "Credenciales invalidas o ausentes.");
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
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // El carrito depende de la cookie de sesion ademas del header Basic (ver Javadoc).
        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    /**
     * Provider de autenticacion contra {@code usuarios}. Combina el {@link UserDetailsService} que
     * resuelve la credencial por email con el {@link PasswordEncoder} <b>reutilizado</b> del
     * componente {@code usuarios} (inyectado por tipo): asi hashear en el alta y verificar en el
     * login usan siempre el mismo algoritmo, sin declarar un segundo encoder.
     */
    @Bean
    DaoAuthenticationProvider proveedorDeAutenticacion(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
