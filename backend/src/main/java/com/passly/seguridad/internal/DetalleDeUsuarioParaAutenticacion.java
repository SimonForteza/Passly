package com.passly.seguridad.internal;

import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.autenticacion.CredencialDTO;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Puente entre Spring Security y el componente {@code usuarios}: dado un email, entrega el
 * {@link UserDetails} que el {@code DaoAuthenticationProvider} necesita para verificar el login.
 *
 * <p>Consume {@link UsuarioService#buscarCredencialPorEmail(String)} (contrato
 * {@code usuarios :: autenticacion}): no ve la entidad ni el repositorio de usuarios, solo el
 * {@link CredencialDTO}. La verificacion del hash la hace Spring Security con el
 * {@code PasswordEncoder} reutilizado de {@code usuarios}, no este puente.
 *
 * <p>Package-private y en {@code internal}: nadie fuera del modulo lo nombra; Spring lo registra
 * igual como bean.
 */
@Service
class DetalleDeUsuarioParaAutenticacion implements UserDetailsService {

    private final UsuarioService usuarioService;

    DetalleDeUsuarioParaAutenticacion(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        CredencialDTO credencial = usuarioService.buscarCredencialPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese email"));

        // El rol del dominio se mapea a una authority con prefijo ROLE_, que es lo que espera
        // hasRole('X') en los @PreAuthorize (hasRole agrega ROLE_ por convencion).
        return User.withUsername(credencial.email())
                .password(credencial.passwordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + credencial.rol().name()))
                .build();
    }
}
