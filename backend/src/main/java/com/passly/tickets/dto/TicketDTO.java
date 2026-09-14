package com.passly.tickets.dto;

import com.passly.tickets.EstadoTicket;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un ticket individual y su QR listo para mostrar en la app web. */
public record TicketDTO(
        Long id,
        UUID codigo,
        Long idOrden,
        Long idEvento,
        Long idTipoEntrada,
        String nombreTipoEntrada,
        EstadoTicket estado,
        OffsetDateTime emitidoEn,
        String qrBase64
) {
}
