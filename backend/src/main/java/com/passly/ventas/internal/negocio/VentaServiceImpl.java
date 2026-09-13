package com.passly.ventas.internal.negocio;

import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.EventoService;
import com.passly.usuarios.UsuarioService;
import com.passly.usuarios.dto.UsuarioDTO;
import com.passly.ventas.OrdenNoEncontradaException;
import com.passly.ventas.VentaService;
import com.passly.ventas.dto.AgregarItemRequest;
import com.passly.ventas.dto.CarritoDTO;
import com.passly.ventas.dto.CompradorDeOrdenDTO;
import com.passly.ventas.dto.ItemDeCarritoDTO;
import com.passly.ventas.dto.OrdenDTO;
import com.passly.ventas.internal.datos.Orden;
import com.passly.ventas.internal.datos.OrdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementacion de {@link VentaService}: el Facade de la compra (CLAUDE.md 4.9).
 *
 * <p><b>Package-private a proposito</b>, igual que {@code EventoServiceImpl} (CLAUDE.md 4.3,
 * regla 1).
 *
 * <p><b>Deliberadamente SIN {@code @Transactional} de clase.</b> Es la diferencia con
 * {@code EventoServiceImpl}, y no un descuido: {@link #confirmarCompra} tiene que cobrar
 * <i>antes</i> de tocar la base (CLAUDE.md 2), y si esta clase fuera transaccional, Spring
 * abririia la transaccion en la primera llamada al metodo — antes de que el codigo del metodo
 * llegue siquiera a la linea del cobro. El limite transaccional real vive en
 * {@link ConfirmacionDeCompra}, un bean aparte al que se llega recien despues de cobrar (ver su
 * Javadoc para el motivo exacto).
 */
@Service
class VentaServiceImpl implements VentaService {

    private final CarritoDeCompra carrito;
    private final EventoService eventoService;
    private final UsuarioService usuarioService;
    private final PasarelaDePago pasarelaDePago;
    private final ConfirmacionDeCompra confirmacionDeCompra;
    private final OrdenRepository ordenRepository;
    private final VentaMapper mapper;

    VentaServiceImpl(
            CarritoDeCompra carrito,
            EventoService eventoService,
            UsuarioService usuarioService,
            PasarelaDePago pasarelaDePago,
            ConfirmacionDeCompra confirmacionDeCompra,
            OrdenRepository ordenRepository,
            VentaMapper mapper
    ) {
        this.carrito = carrito;
        this.eventoService = eventoService;
        this.usuarioService = usuarioService;
        this.pasarelaDePago = pasarelaDePago;
        this.confirmacionDeCompra = confirmacionDeCompra;
        this.ordenRepository = ordenRepository;
        this.mapper = mapper;
    }

    @Override
    public CarritoDTO agregarItem(AgregarItemRequest solicitud, Long idComprador) {
        // El precio y el nombre se congelan aca, contra la misma consulta que va a usar
        // ServicioDeVentas para descontar cupo al confirmar (EventoService.consultarDisponibilidad):
        // el carrito nunca lee el esquema eventos por su cuenta.
        DisponibilidadDTO disponibilidad = eventoService.consultarDisponibilidad(solicitud.idTipoEntrada());

        carrito.agregarItem(
                idComprador,
                disponibilidad.idTipoEntrada(),
                disponibilidad.idEvento(),
                disponibilidad.nombreTipoEntrada(),
                disponibilidad.precio(),
                solicitud.cantidad()
        );
        return carrito.verComo(idComprador);
    }

    @Override
    public CarritoDTO verCarrito(Long idComprador) {
        return carrito.verComo(idComprador);
    }

    @Override
    public OrdenDTO confirmarCompra(Long idComprador) {
        // Lee el carrito SIN vaciarlo: si el cobro o la transaccion fallan mas abajo, el
        // comprador tiene que poder reintentar sin haber perdido su seleccion.
        List<ItemDeCarritoDTO> items = carrito.itemsParaConfirmar(idComprador);
        BigDecimal total = carrito.total();

        // Fuera de cualquier transaccion de base de datos (CLAUDE.md 2 y 4.11): un externo
        // lento no puede mantener una fila bloqueada, y este cobro no es rollbackeable.
        ComprobanteDeCobroDTO comprobante = pasarelaDePago.cobrar(total);

        try {
            Orden orden = confirmacionDeCompra.confirmar(
                    idComprador, items, total, comprobante.id(), comprobante.autorizadoEn());
            carrito.vaciar(idComprador);
            return mapper.aDTO(orden, resolverComprador(idComprador));
        } catch (RuntimeException fallaLaConfirmacion) {
            // El mismo problema de doble escritura que en la Obligatoria 2 resuelve la cola con
            // reintento (CLAUDE.md 2): el cobro ya se aprobo y la orden no se pudo registrar. Se
            // compensa best-effort; si revertir tambien fallara, queda un cobro huerfano y solo
            // un log en ERROR (PasarelaDePago.revertir, Javadoc) -- deuda declarada, no un caso
            // cubierto. El carrito no se vacia: sigue intacto para reintentar.
            pasarelaDePago.revertir(comprobante.id());
            throw fallaLaConfirmacion;
        }
    }

    /**
     * {@code @Transactional(readOnly = true)}, a diferencia del resto de esta clase: sin ella, el
     * {@code EntityManager} que trajo la orden se cierra en cuanto {@code ordenRepository}
     * retorna, y {@code Orden.items} (lazy, como {@code TipoEntrada} en Eventos) explota con
     * {@code LazyInitializationException} apenas el mapper la toca. No contradice el "sin
     * {@code @Transactional} de clase" del Javadoc de la clase: esta anotacion no envuelve
     * ningun cobro, solo una lectura.
     */
    @Override
    @Transactional(readOnly = true)
    public OrdenDTO consultarOrden(Long idOrden, Long idComprador) {
        Orden orden = ordenRepository.findByIdAndCompradorId(idOrden, idComprador)
                .orElseThrow(() -> new OrdenNoEncontradaException(idOrden));
        return mapper.aDTO(orden, resolverComprador(idComprador));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenDTO> listarOrdenesDeComprador(Long idComprador) {
        CompradorDeOrdenDTO comprador = resolverComprador(idComprador);
        return ordenRepository.findByCompradorIdOrderByCreadaEnDesc(idComprador).stream()
                .map(orden -> mapper.aDTO(orden, comprador))
                .toList();
    }

    private CompradorDeOrdenDTO resolverComprador(Long idComprador) {
        UsuarioDTO usuario = usuarioService.consultarUsuario(idComprador);
        return new CompradorDeOrdenDTO(usuario.id(), usuario.email(), usuario.nombre());
    }
}
