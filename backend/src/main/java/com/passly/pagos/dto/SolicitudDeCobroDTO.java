package com.passly.pagos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Pedido de cobro contra la pasarela de pago.
 *
 * <p>Las restricciones de aca son de <b>forma</b> y las verifica la capa de presentacion con
 * {@code @Valid} (CLAUDE.md 4.5) — igual que {@code eventos.dto.CrearEventoRequest}.
 *
 * <p>{@code BigDecimal} para el monto, nunca {@code double}: es dinero, y el punto flotante
 * binario no puede representar exactamente valores decimales.
 *
 * <p><b>La {@code referencia} es del que pide el cobro</b> (hoy quien llama al endpoint; mañana
 * {@code ServicioDeVentas} con el id de su orden), no algo que Pagos invente. Es lo que permite
 * conciliar despues un cobro contra el pedido que lo origino, del lado de quien pidio el cobro.
 */
public record SolicitudDeCobroDTO(

        @NotNull(message = "el monto es obligatorio")
        @DecimalMin(value = "0.01", message = "el monto debe ser mayor a cero")
        BigDecimal monto,

        @NotBlank(message = "la moneda es obligatoria")
        @Size(min = 3, max = 3, message = "la moneda es un codigo ISO 4217 de 3 letras, por ejemplo ARS")
        String moneda,

        @NotBlank(message = "la referencia es obligatoria")
        @Size(max = 100, message = "la referencia no puede superar los 100 caracteres")
        String referencia,

        @NotNull(message = "los datos de la tarjeta son obligatorios")
        @Valid
        DatosDeTarjetaDTO tarjeta
) {
}
