package com.passly.eventos.internal.datos;

import com.passly.eventos.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Patron DAO / Repository sobre la raiz del agregado.
 *
 * <p>Expone el almacenamiento como una coleccion de objetos de dominio, de modo que la capa de
 * negocio no sabe que abajo hay una tabla. La alternativa —inyectar {@code EntityManager} o
 * {@code JdbcTemplate} en el servicio— funciona, pero mezcla consultas con reglas y ata las
 * reglas al mecanismo de persistencia.
 *
 * <p>Que no exista una clase de implementacion es cosa del framework: Spring Data la genera por
 * proxy en runtime. El patron sigue siendo DAO.
 */
public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Cartelera publica: los eventos de un estado dado, del mas proximo al mas lejano. */
    List<Evento> findByEstadoOrderByFechaHoraAsc(EstadoEvento estado);

    /** Cartelera publica filtrada por productora: lo que ve el comprador al elegir un organizador. */
    List<Evento> findByEstadoAndProductoraIdOrderByFechaHoraAsc(
            EstadoEvento estado, Long productoraId);

    /**
     * Backoffice: <b>todos</b> los eventos de una productora, incluidos los BORRADOR.
     *
     * <p>Es la unica consulta que devuelve borradores, y por eso el servicio exige que quien
     * pregunta pueda gestionar esa productora: el borrador de una fiesta todavia no anunciada es
     * informacion comercial sensible.
     */
    List<Evento> findByProductoraIdOrderByFechaHoraAsc(Long productoraId);
}
