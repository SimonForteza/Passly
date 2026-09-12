package com.passly.eventos.dto;

/**
 * Quien organiza un evento, tal como lo ve el comprador en la cartelera.
 *
 * <p><b>Por que un record propio y no el {@code ProductoraDTO} de Productoras:</b> si
 * {@link EventoDTO} expusiera directamente el DTO del otro modulo, todo consumidor de
 * {@code eventos :: dto} — la app web, y manana ServicioDeVentas — quedaria atado tambien a
 * {@code productoras :: dto}. Y el dia que una productora sume un campo fiscal o un logo, cambiaria
 * el contrato de salida de Eventos sin que Eventos haya tocado nada.
 *
 * <p>El costo es un mapeo extra y dos campos duplicados. Es barato: la cartelera necesita exactamente
 * esto — con que nombre mostrar la fiesta y a que id filtrar — y nada mas.
 */
public record OrganizadorDeEventoDTO(
        Long id,
        String nombreComercial
) {
}
