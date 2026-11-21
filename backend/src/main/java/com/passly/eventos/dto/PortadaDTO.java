package com.passly.eventos.dto;

/**
 * La imagen de portada de un evento, lista para servir (PAS-47).
 *
 * @param contenido     los bytes de la imagen
 * @param tipoContenido {@code image/jpeg}, {@code image/png} o {@code image/webp}
 * @param version       cambia con cada subida; alimenta el ETag y la URL versionada
 * @param publica       si el evento ya fue anunciado (no es borrador): decide si la respuesta se
 *                      puede cachear en cualquier lado o solo en el navegador de quien la gestiona
 */
public record PortadaDTO(byte[] contenido, String tipoContenido, long version, boolean publica) {
}
