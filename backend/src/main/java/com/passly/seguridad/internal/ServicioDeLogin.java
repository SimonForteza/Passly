package com.passly.seguridad.internal;

import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.autenticacion.CredencialDTO;
import com.passly.usuarios.dto.UsuarioDTO;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifica email + contraseña y emite el JWT (PAS-33). Es lo unico de Passly que vuelve a ver la
 * contraseña despues del alta: el resto de las requests viaja con el token.
 *
 * <p>Usa el <b>mismo {@code PasswordEncoder}</b> que declara {@code usuarios} (inyectado por tipo),
 * de modo que hashear en el alta y verificar en el login usen siempre el mismo algoritmo.
 *
 * <p><b>Mismo mensaje y mismo costo para "no existe" y "clave incorrecta".</b> Si el email no esta
 * registrado se compara igual contra un hash de relleno: sin eso, la respuesta rapida delataria
 * que la cuenta no existe (enumeracion de usuarios por tiempo de respuesta).
 */
@Service
class ServicioDeLogin {

    static final String CREDENCIALES_INVALIDAS = "Email o contraseña inválidos.";

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration duracion;
    private final Clock reloj;
    private final String hashDeRelleno;

    @Autowired
    ServicioDeLogin(
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${passly.seguridad.jwt.duracion}") String duracion
    ) {
        this(usuarioService, passwordEncoder, jwtEncoder, DurationStyle.detectAndParse(duracion),
                Clock.systemUTC());
    }

    ServicioDeLogin(
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Duration duracion,
            Clock reloj
    ) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.duracion = duracion;
        this.reloj = reloj;
        this.hashDeRelleno = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    SesionIniciada iniciarSesion(String email, String password) {
        Optional<CredencialDTO> credencial = usuarioService.buscarCredencialPorEmail(email);
        String hash = credencial.map(CredencialDTO::passwordHash).orElse(hashDeRelleno);
        boolean coincide = passwordEncoder.matches(password, hash);
        if (credencial.isEmpty() || !coincide) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        UsuarioDTO usuario = usuarioService.consultarUsuario(credencial.get().idUsuario());
        Instant ahora = reloj.instant();
        Instant expiraEn = ahora.plus(duracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ConfiguracionDeJwt.EMISOR)
                .subject(String.valueOf(usuario.id()))
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .id(UUID.randomUUID().toString())
                // Informativos para el cliente: la autorizacion lee el rol vigente de Usuarios
                // (ver ConvertidorDeJwtAAutenticacion), no este claim.
                .claim("email", usuario.email())
                .claim("rol", usuario.rol().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new SesionIniciada(token, expiraEn, usuario);
    }

    record SesionIniciada(String token, Instant expiraEn, UsuarioDTO usuario) {
    }
}
