package com.passly.eventos.internal.negocio;

import com.passly.eventos.EstadoEvento;
import com.passly.eventos.EventoNoEncontradoException;
import com.passly.eventos.EventoService;
import com.passly.eventos.ProductoraNoGestionableException;
import com.passly.eventos.TipoEntradaNoEncontradoException;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;
import com.passly.eventos.dto.OrganizadorDeEventoDTO;
import com.passly.eventos.internal.datos.Evento;
import com.passly.eventos.internal.datos.EventoRepository;
import com.passly.eventos.internal.datos.TipoEntrada;
import com.passly.eventos.internal.datos.TipoEntradaRepository;
import com.passly.productoras.ProductoraService;
import com.passly.productoras.dto.ProductoraDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementacion de {@link EventoService}: las reglas del dominio y el limite transaccional.
 *
 * <p><b>Package-private a proposito.</b> Spring la instancia igual por reflexion; los demas
 * componentes solo pueden inyectar {@link EventoService}, sin poder nombrar esta clase concreta
 * (CLAUDE.md 4.3, regla 1).
 *
 * <p><b>Es el punto donde Eventos consume a Productoras</b>, para dos cosas distintas: autorizar las
 * operaciones de gestion y resolver el nombre comercial con el que se muestra cada fiesta. Esa
 * dependencia esta declarada en el {@code package-info} del modulo, y hasta que se la declaro el
 * build fallaba.
 */
@Service
@Transactional
class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final TipoEntradaRepository tipoEntradaRepository;
    private final ProductoraService productoraService;
    private final EventoMapper mapper;

    EventoServiceImpl(
            EventoRepository eventoRepository,
            TipoEntradaRepository tipoEntradaRepository,
            ProductoraService productoraService,
            EventoMapper mapper
    ) {
        this.eventoRepository = eventoRepository;
        this.tipoEntradaRepository = tipoEntradaRepository;
        this.productoraService = productoraService;
        this.mapper = mapper;
    }

    @Override
    public EventoDTO crearEvento(CrearEventoRequest solicitud, Long idUsuarioActuante) {
        // Un id de productora inexistente tambien cae aca: puedeGestionarEventos da false. No hay
        // forma de crear un evento colgado de una productora que no existe.
        if (!productoraService.puedeGestionarEventos(solicitud.idProductora(), idUsuarioActuante)) {
            throw ProductoraNoGestionableException.alGestionar(
                    solicitud.idProductora(), idUsuarioActuante);
        }

        Evento evento = new Evento(
                solicitud.idProductora(),
                solicitud.nombre(),
                solicitud.descripcion(),
                solicitud.fechaHora(),
                solicitud.lugar()
        );

        for (CrearTipoEntradaRequest tipo : solicitud.tiposEntrada()) {
            evento.agregarTipoEntrada(
                    new TipoEntrada(tipo.nombre(), tipo.precio(), tipo.cupoTotal())
            );
        }

        return conOrganizador(eventoRepository.save(evento));
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDTO consultarEvento(Long idEvento) {
        return conOrganizador(buscarEvento(idEvento));
    }

    @Override
    public EventoDTO publicarEvento(Long idEvento, Long idUsuarioActuante) {
        Evento evento = buscarEvento(idEvento);
        exigirGestionSobre(evento, idUsuarioActuante);

        // La regla de negocio vive en la entidad (Evento.publicar()), no aca: el servicio orquesta
        // y autoriza, el agregado decide si la transicion es valida.
        evento.publicar();
        return conOrganizador(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarEventosPublicados() {
        return conOrganizadores(
                eventoRepository.findByEstadoOrderByFechaHoraAsc(EstadoEvento.PUBLICADO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarEventosPublicadosDeProductora(Long idProductora) {
        return conOrganizadores(eventoRepository.findByEstadoAndProductoraIdOrderByFechaHoraAsc(
                EstadoEvento.PUBLICADO, idProductora));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarEventosDeProductora(Long idProductora, Long idUsuarioActuante) {
        // Devuelve borradores, asi que exige gestion sobre la productora. Un id inexistente cae
        // aca tambien: puedeGestionarEventos da false.
        if (!productoraService.puedeGestionarEventos(idProductora, idUsuarioActuante)) {
            throw ProductoraNoGestionableException.alGestionar(idProductora, idUsuarioActuante);
        }
        return conOrganizadores(
                eventoRepository.findByProductoraIdOrderByFechaHoraAsc(idProductora));
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadDTO consultarDisponibilidad(Long idTipoEntrada) {
        TipoEntrada tipoEntrada = tipoEntradaRepository.findById(idTipoEntrada)
                .orElseThrow(() -> new TipoEntradaNoEncontradoException(idTipoEntrada));

        return new DisponibilidadDTO(
                tipoEntrada.getId(),
                tipoEntrada.getEvento().getId(),
                tipoEntrada.getNombre(),
                tipoEntrada.getPrecio(),
                tipoEntrada.getCupoDisponible(),
                tipoEntrada.tieneCupo()
        );
    }

    private Evento buscarEvento(Long idEvento) {
        return eventoRepository.findById(idEvento)
                .orElseThrow(() -> new EventoNoEncontradoException(idEvento));
    }

    /**
     * Guard de autorizacion sobre un evento existente.
     *
     * <p>Vive en el servicio y no en la entidad porque necesita preguntarle a Productoras quien
     * gestiona esa productora: conocimiento externo al agregado. {@code Evento.publicar()} si esta
     * en la entidad, porque sus tres reglas usan solo datos propios. Inyectar un servicio en una
     * entidad JPA la convertiria en un objeto de servicio con estado persistente.
     */
    private void exigirGestionSobre(Evento evento, Long idUsuarioActuante) {
        if (!productoraService.puedeGestionarEventos(
                evento.getProductoraId(), idUsuarioActuante)) {
            throw ProductoraNoGestionableException.alOperarSobreEvento(
                    evento.getId(), evento.getProductoraId(), idUsuarioActuante);
        }
    }

    /** Resuelve el organizador de un solo evento: una consulta. */
    private EventoDTO conOrganizador(Evento evento) {
        ProductoraDTO productora = productoraService.consultarProductora(evento.getProductoraId());
        return mapper.aDTO(evento, new OrganizadorDeEventoDTO(
                productora.id(), productora.nombreComercial()));
    }

    /**
     * Resuelve el organizador de una lista de eventos con <b>una sola</b> consulta a Productoras,
     * en vez de una por evento.
     *
     * <p>Es la forma de que la cartelera no sea un N+1. Hoy el catalogo de productoras es chico y
     * traerlo entero es lo mas barato; cuando deje de serlo, la evolucion conocida es pedirle a
     * Productoras solo los ids que aparecen en la lista. Que el mapper reciba el organizador ya
     * resuelto es lo que hace que esta decision este a la vista en un solo lugar.
     */
    private List<EventoDTO> conOrganizadores(List<Evento> eventos) {
        Map<Long, ProductoraDTO> porId = productoraService.listarProductoras().stream()
                .collect(Collectors.toMap(ProductoraDTO::id, Function.identity()));

        return eventos.stream()
                .map(evento -> {
                    ProductoraDTO productora = porId.get(evento.getProductoraId());
                    return mapper.aDTO(evento, new OrganizadorDeEventoDTO(
                            productora.id(), productora.nombreComercial()));
                })
                .toList();
    }
}
