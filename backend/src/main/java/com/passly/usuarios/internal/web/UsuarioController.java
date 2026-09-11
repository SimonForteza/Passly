package com.passly.usuarios.internal.web;

import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Traduce HTTP a llamadas sobre {@link UsuarioService}. Sin logica de dominio y sin conocer la
 * entidad: solo trata con la interfaz de negocio y los DTOs (CLAUDE.md 4.5).
 *
 * <p>Package-private: nada fuera de este paquete necesita nombrarla, Spring la registra igual
 * como bean {@code @RestController}.
 */
@RestController
class UsuarioController {

    private final UsuarioService usuarioService;

    UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // El alta es publica solo para COMPRADOR; los demas roles los da de alta un ADMIN autenticado.
    // El "rol != null" es defensa en profundidad: un rol nulo ya cae en 400 por @NotNull (la
    // validacion de @RequestBody corre antes que este chequeo), pero evita un NPE en el SpEL si
    // alguna vez la validacion no se aplicara.
    @PostMapping("/api/usuarios")
    @PreAuthorize("#solicitud.rol != null and (#solicitud.rol.name() == 'COMPRADOR' or hasRole('ADMIN'))")
    ResponseEntity<UsuarioDTO> registrarUsuario(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        UsuarioDTO creado = usuarioService.registrarUsuario(solicitud);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    @GetMapping("/api/usuarios/{id}")
    UsuarioDTO consultarUsuario(@PathVariable("id") Long id) {
        return usuarioService.consultarUsuario(id);
    }

    @GetMapping("/api/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    List<UsuarioDTO> listarUsuarios() {
        return usuarioService.listarUsuarios();
    }
}
