package com.passly.ventas.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Patron DAO / Repository sobre la raiz del agregado, igual que en Eventos. */
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /**
     * Una orden por id, solo si es de ese comprador. Una sola consulta en vez de
     * {@code findById} + comparar en memoria: evita cargar la orden de otro comprador para
     * despues descartarla.
     */
    Optional<Orden> findByIdAndCompradorId(Long id, Long compradorId);

    /** Las ordenes de un comprador, de la mas reciente a la mas antigua. */
    List<Orden> findByCompradorIdOrderByCreadaEnDesc(Long compradorId);
}
