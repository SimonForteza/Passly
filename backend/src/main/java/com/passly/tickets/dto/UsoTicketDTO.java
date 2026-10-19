package com.passly.tickets.dto;

import java.time.OffsetDateTime;

/** Resultado de consumo. No expone QR, comprador ni datos de la orden. */
public record UsoTicketDTO(Resultado resultado, Long idTicket, Long idEvento,
                           String nombreTipoEntrada, OffsetDateTime usadoEn) {
    public enum Resultado { VALIDO, YA_USADO, OTRO_EVENTO, INVALIDO }
}
