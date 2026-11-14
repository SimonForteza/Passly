package com.passly.seguridad.internal;

import com.passly.usuarios.UsuarioNoEncontradoException;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT ya validado (firma, emisor y vencimiento) en la {@code Authentication} con la
 * que trabajan los controllers.
 *
 * <p><b>El nombre del principal es el id del usuario</b> (claim {@code sub}), igual que con HTTP
 * Basic, donde el {@code UserDetails} llevaba el id como username (CLAUDE.md 4.11). Eventos,
 * Productoras, Ventas y Accesos leen {@code Authentication#getName()} y lo parsean como
 * {@code Long}: pasar a JWT no les cambia nada, y Eventos sigue sin depender de Usuarios.
 *
 * <p><b>El rol se lee actualizado de Usuarios en cada request, no del token.</b> El rol global
 * cambia durante la sesion: crear una productora habilita {@code ORGANIZADOR}, y aceptar una
 * invitacion puede habilitar {@code ORGANIZADOR} o {@code VALIDADOR}
 * ({@code UsuarioService#habilitarCapacidad}). Si la authority saliera del claim {@code rol}, quien
 * acaba de crear su productora seguiria siendo {@code COMPRADOR} hasta volver a iniciar sesion, y
 * crear su primer evento le daria 403. El token prueba <i>quien es</i>; <i>que puede hacer</i> se
 * consulta en la fuente de verdad, como hacia Basic, pero sin el costo de BCrypt en cada request.
 * Si la cuenta ya no existe, el token deja de servir (401).
 */
@Component
class ConvertidorDeJwtAAutenticacion implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioService usuarioService;

    ConvertidorDeJwtAAutenticacion(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UsuarioDTO usuario = buscarTitular(jwt.getSubject());
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())),
                String.valueOf(usuario.id()));
    }

    private UsuarioDTO buscarTitular(String subject) {
        try {
            return usuarioService.consultarUsuario(Long.valueOf(subject));
        } catch (NumberFormatException | UsuarioNoEncontradoException e) {
            // AuthenticationException: el filtro de Bearer la traduce a 401, nunca a 500.
            throw new InvalidBearerTokenException("El token no corresponde a una cuenta vigente");
        }
    }
}
