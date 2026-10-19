package com.passly.tickets;

/** Estado de ciclo de vida del ticket. Accesos solicita la transicion atomica a USADO. */
public enum EstadoTicket {
    EMITIDO,
    USADO
}
