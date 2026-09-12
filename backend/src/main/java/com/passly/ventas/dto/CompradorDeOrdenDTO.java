package com.passly.ventas.dto;

/**
 * Datos del comprador embebidos en {@link OrdenDTO}, resueltos contra {@code UsuarioService}.
 *
 * <p>Mismo patron que {@code OrganizadorDeEventoDTO} en Eventos: existe para no exponer
 * {@code UsuarioDTO} completo (ni acoplar el contrato de Ventas a como Usuarios modela su DTO)
 * dentro de la respuesta de una orden.
 */
public record CompradorDeOrdenDTO(Long id, String email, String nombre) {
}
