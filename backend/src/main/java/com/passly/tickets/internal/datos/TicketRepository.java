package com.passly.tickets.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    java.util.Optional<Ticket> findByContenidoQr(String contenidoQr);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
        update Ticket t set t.estado = com.passly.tickets.EstadoTicket.USADO, t.usadoEn = :momento
        where t.contenidoQr = :contenido and t.eventoId = :evento
        and t.estado = com.passly.tickets.EstadoTicket.EMITIDO
        """)
    int consumir(@org.springframework.data.repository.query.Param("contenido") String contenido,
                 @org.springframework.data.repository.query.Param("evento") Long evento,
                 @org.springframework.data.repository.query.Param("momento") java.time.OffsetDateTime momento);

    List<Ticket> findByOrdenIdAndCompradorIdOrderByIdAsc(Long ordenId, Long compradorId);
}
