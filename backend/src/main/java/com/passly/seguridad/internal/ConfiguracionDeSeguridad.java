package com.passly.seguridad.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
 */
@Configuration
@EnableMethodSecurity
class ConfiguracionDeSeguridad {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                // API REST sin cookies de sesion: CSRF no aplica (no hay sesion que falsificar).
                .csrf(csrf -> csrf.disable())
                // Sin estado de sesion en el servidor: cada request se autentica con sus credenciales.
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
                .httpBasic(Customizer.withDefaults());

        return http.build();
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
