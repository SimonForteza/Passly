package com.passly.eventos.internal.datos;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a tipos de entrada por id.
 *
 * <p><b>Tension conocida, aceptada a proposito:</b> {@code TipoEntrada} no es raiz de agregado,
 * asi que en DDD estricto no deberia tener repositorio propio. Se acepta porque
 * {@code consultarDisponibilidad} es una <i>lectura</i> por id — no una mutacion — y porque
 * {@code ServicioDeVentas} ya conoce ese id: se lo dio la cartelera. Cualquier cambio sobre el
 * cupo si va a pasar por la raiz.
 */
public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {
}
