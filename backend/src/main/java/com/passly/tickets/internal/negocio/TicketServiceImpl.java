package com.passly.tickets.internal.negocio;

import com.passly.tickets.TicketService;
import com.passly.tickets.dto.EmitirTicketsRequest;
import com.passly.tickets.dto.TicketDTO;
import com.passly.tickets.internal.datos.Ticket;
import com.passly.tickets.internal.datos.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class TicketServiceImpl implements TicketService {

    private final TicketRepository repository;
    private final GeneradorDeQr generadorDeQr;
    private final TicketMapper mapper;

    TicketServiceImpl(TicketRepository repository, GeneradorDeQr generadorDeQr, TicketMapper mapper) {
        this.repository = repository;
        this.generadorDeQr = generadorDeQr;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void emitirTickets(EmitirTicketsRequest solicitud) {
        List<Ticket> nuevos = new ArrayList<>();
        for (var linea : solicitud.lineas()) {
            for (int numero = 0; numero < linea.cantidad(); numero++) {
                UUID codigo = UUID.randomUUID();
                nuevos.add(new Ticket(
                        codigo,
                        solicitud.idOrden(),
                        solicitud.idComprador(),
                        linea.idEvento(),
                        linea.idTipoEntrada(),
                        linea.nombreTipoEntrada(),
                        generadorDeQr.crearContenido(codigo)));
            }
        }
        repository.saveAll(nuevos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDTO> listarTicketsDeOrden(Long idOrden, Long idComprador) {
        return repository.findByOrdenIdAndCompradorIdOrderByIdAsc(idOrden, idComprador).stream()
                .map(mapper::aDTO)
                .toList();
    }

    @Override
    @Transactional
    public com.passly.tickets.dto.UsoTicketDTO validarYUsar(String contenidoQr, Long idEvento) {
        var invalido = new com.passly.tickets.dto.UsoTicketDTO(
                com.passly.tickets.dto.UsoTicketDTO.Resultado.INVALIDO, null, null, null, null);
        if (!generadorDeQr.esValido(contenidoQr)) return invalido;
        var encontrado = repository.findByContenidoQr(contenidoQr);
        if (encontrado.isEmpty()) return invalido;
        Ticket ticket = encontrado.get();
        if (!ticket.getEventoId().equals(idEvento)) {
            // El contenido existe, pero no se entrega informacion del evento ajeno.
            return new com.passly.tickets.dto.UsoTicketDTO(
                    com.passly.tickets.dto.UsoTicketDTO.Resultado.OTRO_EVENTO, ticket.getId(), null, null, null);
        }
        int consumidos = repository.consumir(contenidoQr, idEvento, java.time.OffsetDateTime.now());
        Ticket actualizado = repository.findByContenidoQr(contenidoQr).orElseThrow();
        return new com.passly.tickets.dto.UsoTicketDTO(consumidos == 1
                ? com.passly.tickets.dto.UsoTicketDTO.Resultado.VALIDO
                : com.passly.tickets.dto.UsoTicketDTO.Resultado.YA_USADO,
                actualizado.getId(), actualizado.getEventoId(), actualizado.getNombreTipoEntrada(), actualizado.getUsadoEn());
    }

    @Override
    public boolean esContenidoQrValido(String contenidoQr) {
        return generadorDeQr.esValido(contenidoQr);
    }
}
