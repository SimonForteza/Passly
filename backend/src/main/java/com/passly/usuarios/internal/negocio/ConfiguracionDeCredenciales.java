package com.passly.usuarios.internal.negocio;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provee el {@link PasswordEncoder} del componente Usuarios.
 *
 * <p><b>El bean vive adentro de Usuarios a proposito.</b> No es configuracion de seguridad
 * (no hay filter chain ni autorizacion, eso es PAS-6): es solo la estrategia de hashing que
 * el propio componente usa para guardar la credencial. Tenerlo aca deja a PAS-5 self-contained,
 * y en PAS-6 Spring Security reutiliza <b>este mismo</b> {@code PasswordEncoder} para verificar
 * el login, de modo que hashear y verificar usen siempre el mismo algoritmo.
 *
 * <p>Package-private y en {@code internal}: Spring lo registra por reflexion, ningun otro modulo
 * lo ve.
 */
@Configuration
class ConfiguracionDeCredenciales {

    /**
     * BCrypt: funcion de hashing con sal incorporada y costo configurable, pensada para
     * contrasenas. No es un digest generico como SHA-256 — es deliberadamente lento para
     * encarecer el fuerza bruta.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
