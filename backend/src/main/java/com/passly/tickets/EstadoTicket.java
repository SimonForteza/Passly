package com.passly.tickets;

/** Estado de ciclo de vida del ticket. Accesos incorporara la transicion a USADO. */
public enum EstadoTicket {
    EMITIDO,
    USADO
}
