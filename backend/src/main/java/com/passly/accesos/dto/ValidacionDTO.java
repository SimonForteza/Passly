package com.passly.accesos.dto;

import java.time.OffsetDateTime;

public record ValidacionDTO(Resultado resultado, TicketValidado ticket, String mensaje) {
    public enum Resultado { VALIDO, YA_USADO, OTRO_EVENTO, INVALIDO }
    public record TicketValidado(Long id, Long idEvento, String nombreTipoEntrada, OffsetDateTime usadoEn) {}
}
