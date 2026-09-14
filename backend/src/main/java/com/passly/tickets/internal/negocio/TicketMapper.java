package com.passly.tickets.internal.negocio;

import com.passly.tickets.dto.TicketDTO;
import com.passly.tickets.internal.datos.Ticket;
import org.springframework.stereotype.Component;

@Component
class TicketMapper {

    private final GeneradorDeQr generadorDeQr;

    TicketMapper(GeneradorDeQr generadorDeQr) {
        this.generadorDeQr = generadorDeQr;
    }

    TicketDTO aDTO(Ticket ticket) {
        return new TicketDTO(
                ticket.getId(),
                ticket.getCodigo(),
                ticket.getOrdenId(),
                ticket.getEventoId(),
                ticket.getTipoEntradaId(),
                ticket.getNombreTipoEntrada(),
                ticket.getEstado(),
                ticket.getEmitidoEn(),
                generadorDeQr.comoPngBase64(ticket.getContenidoQr()));
    }
}
