package com.passly.pagos.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Patron DAO / Repository sobre {@link Pago}. Que no exista una clase de implementacion es cosa
 * del framework: Spring Data la genera por proxy en runtime (mismo criterio que
 * {@code eventos.internal.datos.EventoRepository}).
 */
public interface PagoRepository extends JpaRepository<Pago, Long> {
}
