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
 *
 * <p><b>Por que el {@code UserDetails} lleva el id y no el email como username.</b> El login sigue
 * siendo por email — {@code loadUserByUsername} recibe el email que mando el cliente en el header
 * Basic — pero {@link User#getUsername()} no tiene que devolver lo mismo que recibio: Spring
 * Security arma el {@code Authentication} final con lo que este metodo devuelva, y despues
 * {@code Authentication#getName()} delega en ese username. Devolviendo el id numerico, cualquier
 * controller de cualquier modulo (Eventos incluido) obtiene el id del actuante leyendo
 * {@code Authentication#getName()} — un tipo de Spring, no de Passly — sin depender de Usuarios
 * para resolver email a id. Eventos en particular no puede hacer esa resolucion: su
 * {@code package-info} no declara la dependencia y el build fallaria si la necesitara.
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
        return User.withUsername(String.valueOf(credencial.idUsuario()))
                .password(credencial.passwordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + credencial.rol().name()))
                .build();
    }
}
