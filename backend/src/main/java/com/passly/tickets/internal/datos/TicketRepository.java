package com.passly.tickets.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByOrdenIdAndCompradorIdOrderByIdAsc(Long ordenId, Long compradorId);
}
