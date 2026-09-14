package com.passly.tickets;

import com.passly.tickets.dto.EmitirTicketsRequest;
import com.passly.tickets.dto.TicketDTO;
import jakarta.validation.Valid;

import java.util.List;

/** Contrato publico de emision y consulta de tickets. */
public interface TicketService {

    /** Emite un ticket individual por cada unidad incluida en la solicitud. */
    void emitirTickets(@Valid EmitirTicketsRequest solicitud);

    /** Lista los tickets de una orden solo cuando pertenecen al comprador indicado. */
    List<TicketDTO> listarTicketsDeOrden(Long idOrden, Long idComprador);

    /** Verifica que el contenido haya sido emitido por Passly y no haya sido alterado. */
    boolean esContenidoQrValido(String contenidoQr);
}
