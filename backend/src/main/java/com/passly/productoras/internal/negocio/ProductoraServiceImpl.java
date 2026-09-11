package com.passly.productoras.internal.negocio;

import com.passly.productoras.NoEsMiembroDeLaProductoraException;
import com.passly.productoras.NombreComercialYaRegistradoException;
import com.passly.productoras.ProductoraNoEncontradaException;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.RolEnProductora;
import com.passly.productoras.RolIncompatibleConLaMembresiaException;
import com.passly.productoras.YaEsMiembroDeLaProductoraException;
import com.passly.productoras.dto.AgregarMiembroRequest;
import com.passly.productoras.dto.CrearProductoraRequest;
import com.passly.productoras.dto.MiembroDTO;
import com.passly.productoras.dto.ProductoraDTO;
import com.passly.productoras.internal.datos.Miembro;
import com.passly.productoras.internal.datos.Productora;
import com.passly.productoras.internal.datos.ProductoraRepository;
import com.passly.usuarios.Rol;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link ProductoraService}: las reglas del dominio y el limite transaccional.
 *
 * <p><b>Package-private a proposito.</b> Spring la instancia igual por reflexion; los demas
 * componentes solo pueden inyectar {@link ProductoraService}, sin poder nombrar esta clase concreta
 * (CLAUDE.md 4.3, regla 1).
 *
 * <p><b>Este es el unico punto del sistema donde se consulta el rol global para decidir una
 * membresia.</b> Despues de que alguien entra al padron, ninguna operacion vuelve a preguntarle a
 * Usuarios: se pregunta por la membresia. La verificacion cara se paga una vez, en el alta.
 */
@Service
@Transactional
class ProductoraServiceImpl implements ProductoraService {

    private final ProductoraRepository productoraRepository;
    private final UsuarioService usuarioService;
    private final ProductoraMapper mapper;

    ProductoraServiceImpl(
            ProductoraRepository productoraRepository,
            UsuarioService usuarioService,
            ProductoraMapper mapper
    ) {
        this.productoraRepository = productoraRepository;
        this.usuarioService = usuarioService;
        this.mapper = mapper;
    }

    @Override
    public ProductoraDTO crearProductora(CrearProductoraRequest solicitud, Long idUsuarioCreador) {
        if (productoraRepository.existsByNombreComercial(solicitud.nombreComercial())) {
            throw new NombreComercialYaRegistradoException(solicitud.nombreComercial());
        }

        // Lanza "usuario no encontrado" (404) si el id no existe, antes de crear nada.
        UsuarioDTO creador = usuarioService.consultarUsuario(idUsuarioCreador);
        exigirRolGlobalCompatible(creador, RolEnProductora.DUENIO);

        Productora productora = new Productora(
                solicitud.nombreComercial(),
                solicitud.cuit(),
                solicitud.descripcion(),
                solicitud.logoUrl()
        );
        // Una productora sin duenio seria un agregado a medias: nadie podria operarla nunca.
        productora.incorporar(new Miembro(idUsuarioCreador, RolEnProductora.DUENIO));

        return mapper.aDTO(productoraRepository.save(productora));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoraDTO consultarProductora(Long idProductora) {
        return mapper.aDTO(buscarProductora(idProductora));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoraDTO> listarProductoras() {
        return productoraRepository.findAllByOrderByNombreComercialAsc().stream()
                .map(mapper::aDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoraDTO> listarProductorasDeUsuario(Long idUsuario) {
        return productoraRepository
                .findByMiembros_UsuarioIdOrderByNombreComercialAsc(idUsuario).stream()
                .map(mapper::aDTO)
                .toList();
    }

    @Override
    public MiembroDTO agregarMiembro(
            Long idProductora,
            AgregarMiembroRequest solicitud,
            Long idUsuarioSolicitante
    ) {
        Productora productora = buscarProductora(idProductora);

        // Gestionar el padron es potestad del DUENIO: el STAFF administra eventos, no personas.
        if (!productora.puedeGestionarMiembros(idUsuarioSolicitante)) {
            throw new NoEsMiembroDeLaProductoraException(idProductora, idUsuarioSolicitante);
        }
        if (productora.tieneMiembro(solicitud.idUsuario())) {
            throw new YaEsMiembroDeLaProductoraException(idProductora, solicitud.idUsuario());
        }

        UsuarioDTO invitado = usuarioService.consultarUsuario(solicitud.idUsuario());
        exigirRolGlobalCompatible(invitado, solicitud.rolEnProductora());

        Miembro miembro = new Miembro(solicitud.idUsuario(), solicitud.rolEnProductora());
        productora.incorporar(miembro);

        return mapper.aDTO(miembro, invitado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiembroDTO> listarMiembros(Long idProductora, Long idUsuarioSolicitante) {
        Productora productora = buscarProductora(idProductora);
        if (!productora.tieneMiembro(idUsuarioSolicitante)) {
            throw new NoEsMiembroDeLaProductoraException(idProductora, idUsuarioSolicitante);
        }

        // Una consulta por miembro. Un padron tiene unidades de personas, asi que N es chico y
        // acotado; traer todos los usuarios del sistema para armar un mapa escalaria peor.
        return productora.getMiembros().stream()
                .map(miembro -> mapper.aDTO(
                        miembro,
                        usuarioService.consultarUsuario(miembro.getUsuarioId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esMiembro(Long idProductora, Long idUsuario) {
        return productoraRepository.findById(idProductora)
                .map(productora -> productora.tieneMiembro(idUsuario))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puedeGestionarEventos(Long idProductora, Long idUsuario) {
        // La regla de que roles califican vive en la entidad (Productora.puedeGestionarEventos):
        // usa solo datos propios del agregado. Aca solo se resuelve el agregado.
        return productoraRepository.findById(idProductora)
                .map(productora -> productora.puedeGestionarEventos(idUsuario))
                .orElse(false);
    }

    private Productora buscarProductora(Long idProductora) {
        return productoraRepository.findById(idProductora)
                .orElseThrow(() -> new ProductoraNoEncontradaException(idProductora));
    }

    /**
     * Cierra los dos ejes: el rol global del usuario tiene que corresponder al rol interno con el
     * que se lo quiere incorporar.
     *
     * <p>La correspondencia vive aca, en negocio, y no en el enum: es la unica regla del sistema que
     * necesita conocer los dos vocabularios a la vez, y {@code RolEnProductora} no deberia saber que
     * existe {@code usuarios.Rol}.
     */
    private void exigirRolGlobalCompatible(UsuarioDTO usuario, RolEnProductora rolEnProductora) {
        Rol requerido = rolGlobalRequeridoPara(rolEnProductora);
        if (usuario.rol() != requerido) {
            throw new RolIncompatibleConLaMembresiaException(
                    usuario.id(), rolEnProductora, requerido.name());
        }
    }

    private Rol rolGlobalRequeridoPara(RolEnProductora rolEnProductora) {
        return rolEnProductora == RolEnProductora.VALIDADOR ? Rol.VALIDADOR : Rol.ORGANIZADOR;
    }
}
