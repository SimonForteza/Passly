package com.passly.usuarios;

import com.passly.usuarios.dto.CrearUsuarioRequest;
import com.passly.usuarios.dto.UsuarioDTO;

import java.util.List;

/**
 * Contrato publico de <b>ServicioDeUsuarios</b>: registro y consulta de usuarios y su rol.
 *
 * <p>Es la unica puerta de entrada al componente. La implementacion, la entidad JPA, el
 * repositorio y el {@code PasswordEncoder} viven en {@code internal} y no forman parte de este
 * contrato; los demas componentes inyectan esta interfaz y no pueden nombrar la clase concreta.
 *
 * <p><b>Sobre la credencial:</b> el password en texto plano entra solo por
 * {@link #registrarUsuario(CrearUsuarioRequest)} y no vuelve a salir: se guarda hasheado con
 * BCrypt dentro del componente y {@link UsuarioDTO} no incluye el hash. Verificar credenciales
 * en el login es responsabilidad de PAS-6 y ocurrira <b>adentro</b> del componente, para que el
 * hash nunca cruce la frontera.
 *
 * <p><b>Posicion en el sistema:</b> Usuarios no llama a ningun otro componente. Es
 * <b>stateless</b>: no guarda estado conversacional entre llamadas (persistir usuarios no lo
 * vuelve stateful, CLAUDE.md 4.7).
 */
public interface UsuarioService {

    /**
     * Da de alta un usuario con su rol. El password llega en texto plano dentro de la solicitud,
     * se hashea con BCrypt y se guarda solo el hash.
     *
     * @throws EmailYaRegistradoException si ya existe una cuenta con ese email
     */
    UsuarioDTO registrarUsuario(CrearUsuarioRequest solicitud);

    /**
     * Devuelve un usuario por id.
     *
     * @throws UsuarioNoEncontradoException si no existe
     */
    UsuarioDTO consultarUsuario(Long idUsuario);

    /**
     * Devuelve un usuario por su email, que es la clave natural de la cuenta.
     *
     * <p>Existe por dos consumidores distintos: PAS-6 lo necesita para el login (el username de
     * Spring Security es el email), y los componentes que resuelven un usuario del que solo
     * conocen el email en vez del id generado.
     *
     * @throws UsuarioNoEncontradoException si no existe
     */
    UsuarioDTO consultarUsuarioPorEmail(String email);

    /** Lista todos los usuarios, ordenados por email ascendente. */
    List<UsuarioDTO> listarUsuarios();
}
