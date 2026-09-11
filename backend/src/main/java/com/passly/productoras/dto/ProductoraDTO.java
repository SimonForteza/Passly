package com.passly.productoras.dto;

/**
 * Vista de salida de una productora: lo que el componente expone al mundo.
 *
 * <p><b>No incluye el padron de miembros.</b> Quien es miembro de una productora es informacion de
 * gestion, no de catalogo: un comprador que mira la cartelera no tiene por que recibir la lista de
 * personas que trabajan ahi. El padron se pide aparte, y ese endpoint exige ser miembro.
 */
public record ProductoraDTO(
        Long id,
        String nombreComercial,
        String cuit,
        String descripcion,
        String logoUrl
) {
}
