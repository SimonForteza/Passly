package com.passly.tickets.internal.web;

import com.passly.tickets.TicketService;
import com.passly.tickets.dto.TicketDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API de consulta de los tickets emitidos para el comprador autenticado. */
@RestController
class TicketController {

    private final TicketService ticketService;

    TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/api/tickets/ordenes/{idOrden}")
    List<TicketDTO> listarPorOrden(
            @PathVariable("idOrden") Long idOrden,
            Authentication authentication
    ) {
        return ticketService.listarTicketsDeOrden(idOrden, Long.valueOf(authentication.getName()));
    }
}
