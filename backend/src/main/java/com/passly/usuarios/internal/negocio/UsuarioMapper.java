package com.passly.usuarios.internal.negocio;

import com.passly.usuarios.dto.UsuarioDTO;
import com.passly.usuarios.internal.datos.Usuario;
import org.springframework.stereotype.Component;

/**
 * Entidad JPA -&gt; DTO. Es <b>el unico lugar del componente donde una entidad y un DTO se
 * tocan</b>: que sea un solo archivo es lo que hace auditable la regla "las entidades JPA nunca
 * salen del componente" (CLAUDE.md 4.3).
 *
 * <p><b>No copia {@code passwordHash}.</b> La credencial no entra en ningun DTO: aca es donde se
 * verifica, de un vistazo, que el hash no cruza la frontera del componente.
 */
@Component
class UsuarioMapper {

    UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRol()
        );
    }
}
