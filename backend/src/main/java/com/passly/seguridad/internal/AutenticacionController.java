package com.passly.seguridad.internal;

import com.passly.usuarios.dto.UsuarioDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/auth/login}: canjea email + contraseña por un JWT (PAS-33).
 *
 * <p>Es publico ({@code permitAll} en {@link ConfiguracionDeSeguridad}). Responde el token, cuando
 * vence y el {@link UsuarioDTO} del titular, que es lo que el frontend necesitaba pedirle a
 * {@code GET /api/usuarios/me} con HTTP Basic. Credenciales invalidas: 401 en
 * {@code ProblemDetail}, con el mismo mensaje exista o no la cuenta.
 */
@RestController
@RequestMapping("/api/auth")
class AutenticacionController {

    private final ServicioDeLogin servicioDeLogin;

    AutenticacionController(ServicioDeLogin servicioDeLogin) {
        this.servicioDeLogin = servicioDeLogin;
    }

    @PostMapping("/login")
    RespuestaDeLogin iniciarSesion(@Valid @RequestBody SolicitudDeLogin solicitud) {
        ServicioDeLogin.SesionIniciada sesion =
                servicioDeLogin.iniciarSesion(solicitud.email().trim(), solicitud.password());
        return new RespuestaDeLogin(sesion.token(), "Bearer", sesion.expiraEn(), sesion.usuario());
    }

    /**
     * El tope de 72 caracteres es el mismo de {@code CrearUsuarioRequest}: BCrypt no considera mas
     * alla de 72 bytes, y una contraseña mas larga no puede haberse registrado.
     */
    record SolicitudDeLogin(
            @NotBlank(message = "El email es obligatorio") String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres") String password
    ) {
    }

    record RespuestaDeLogin(String token, String tipo, Instant expiraEn, UsuarioDTO usuario) {
    }
}
