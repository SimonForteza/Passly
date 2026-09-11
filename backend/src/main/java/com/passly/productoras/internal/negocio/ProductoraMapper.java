package com.passly.productoras.internal.negocio;

import com.passly.productoras.dto.MiembroDTO;
import com.passly.productoras.dto.ProductoraDTO;
import com.passly.productoras.internal.datos.Miembro;
import com.passly.productoras.internal.datos.Productora;
import com.passly.usuarios.dto.UsuarioDTO;
import org.springframework.stereotype.Component;

/**
 * Entidad JPA -&gt; DTO. Es <b>el unico lugar del componente donde una entidad y un DTO se tocan</b>:
 * que sea un solo archivo es lo que hace auditable la regla "las entidades JPA nunca salen del
 * componente" (CLAUDE.md 4.3).
 *
 * <p><b>No recibe {@code UsuarioService} inyectado</b>, a proposito. Para armar un
 * {@link MiembroDTO} hacen falta el email y el nombre del usuario, que viven en otro componente;
 * si el mapper los fuera a buscar por su cuenta, cada mapeo dispararia una consulta escondida y el
 * N+1 quedaria invisible en el codigo. En cambio recibe el {@link UsuarioDTO} ya resuelto: quien
 * llama decide cuantas consultas hace y el costo queda a la vista.
 */
@Component
class ProductoraMapper {

    ProductoraDTO aDTO(Productora productora) {
        return new ProductoraDTO(
                productora.getId(),
                productora.getNombreComercial(),
                productora.getCuit(),
                productora.getDescripcion(),
                productora.getLogoUrl()
        );
    }

    /**
     * @param usuario el usuario ya resuelto contra Usuarios; aporta el email y el nombre que la
     *                entidad {@link Miembro} no guarda
     */
    MiembroDTO aDTO(Miembro miembro, UsuarioDTO usuario) {
        return new MiembroDTO(
                miembro.getUsuarioId(),
                usuario.email(),
                usuario.nombre(),
                miembro.getRolInterno()
        );
    }
}
