package com.passly.usuarios.internal.negocio;

import com.passly.usuarios.EmailYaRegistradoException;
import com.passly.usuarios.UsuarioNoEncontradoException;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.autenticacion.CredencialDTO;
import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;
import com.passly.usuarios.internal.datos.Usuario;
import com.passly.usuarios.internal.datos.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementacion de {@link UsuarioService}: las reglas del dominio y el limite transaccional.
 *
 * <p><b>Package-private a proposito.</b> Spring la instancia igual por reflexion; los demas
 * componentes solo pueden inyectar {@link UsuarioService}, sin poder nombrar esta clase concreta
 * (CLAUDE.md 4.3, regla 1).
 */
@Service
@Transactional
class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;

    UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioMapper mapper,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioDTO registrarUsuario(CrearUsuarioRequest solicitud) {
        if (usuarioRepository.existsByEmail(solicitud.email())) {
            throw new EmailYaRegistradoException(solicitud.email());
        }

        // El texto plano se hashea aca y nunca se guarda ni se loguea: a la entidad y a la base
        // solo llega el hash BCrypt.
        String passwordHash = passwordEncoder.encode(solicitud.password());

        Usuario usuario = new Usuario(
                solicitud.email(),
                solicitud.nombre(),
                passwordHash,
                solicitud.rol()
        );

        return mapper.aDTO(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO consultarUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idUsuario));
        return mapper.aDTO(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAllByOrderByEmailAsc().stream()
                .map(mapper::aDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CredencialDTO> buscarCredencialPorEmail(String email) {
        // Unico lugar donde el hash sale del componente: se arma el CredencialDTO a mano y no con
        // UsuarioMapper (que a proposito nunca lee el hash). El texto plano no interviene aca.
        return usuarioRepository.findByEmail(email)
                .map(u -> new CredencialDTO(u.getEmail(), u.getPasswordHash(), u.getRol()));
    }
}
