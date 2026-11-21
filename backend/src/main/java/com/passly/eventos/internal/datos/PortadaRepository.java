package com.passly.eventos.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

/** DAO de {@link Portada}. La clave es el id del evento. */
public interface PortadaRepository extends JpaRepository<Portada, Long> {
}
