package com.passly.eventos.internal;

import com.passly.eventos.EstadoEvento;
import com.passly.eventos.EventoNoEncontradoException;
import com.passly.eventos.EventoService;
import com.passly.eventos.TipoEntradaNoEncontradoException;
import com.passly.eventos.dto.CrearEventoRequest;
import com.passly.eventos.dto.CrearTipoEntradaRequest;
import com.passly.eventos.dto.DisponibilidadDTO;
import com.passly.eventos.dto.EventoDTO;
import com.passly.eventos.internal.datos.Evento;
import com.passly.eventos.internal.datos.EventoRepository;
import com.passly.eventos.internal.datos.TipoEntrada;
import com.passly.eventos.internal.datos.TipoEntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link EventoService}: las reglas del dominio y el limite transaccional.
 *
 * <p><b>Package-private a proposito.</b> Spring la instancia igual por reflexion; los demas
 * componentes solo pueden inyectar {@link EventoService}, sin poder nombrar esta clase concreta
 * (CLAUDE.md 4.3, regla 1).
 */
@Service
@Transactional
class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final TipoEntradaRepository tipoEntradaRepository;
    private final EventoMapper mapper;

    EventoServiceImpl(
            EventoRepository eventoRepository,
            TipoEntradaRepository tipoEntradaRepository,
            EventoMapper mapper
    ) {
        this.eventoRepository = eventoRepository;
        this.tipoEntradaRepository = tipoEntradaRepository;
        this.mapper = mapper;
    }

    @Override
    public EventoDTO crearEvento(CrearEventoRequest solicitud) {
        Evento evento = new Evento(
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

        Evento guardado = eventoRepository.save(evento);
        return mapper.aDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDTO consultarEvento(Long idEvento) {
        return mapper.aDTO(buscarEvento(idEvento));
    }

    @Override
    public EventoDTO publicarEvento(Long idEvento) {
        Evento evento = buscarEvento(idEvento);
        // La regla vive en la entidad (Evento.publicar()), no aca: el servicio orquesta,
        // el agregado decide.
        evento.publicar();
        return mapper.aDTO(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> listarEventosPublicados() {
        return eventoRepository.findByEstadoOrderByFechaHoraAsc(EstadoEvento.PUBLICADO).stream()
                .map(mapper::aDTO)
                .toList();
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
}
