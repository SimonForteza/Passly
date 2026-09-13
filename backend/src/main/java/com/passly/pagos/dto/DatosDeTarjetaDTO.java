package com.passly.pagos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de la tarjeta con la que se paga.
 *
 * <p>Viaja hasta el Adapter y de ahi a la pasarela externa, pero <b>nunca se persiste</b>: la
 * entidad {@code Pago} solo guarda el resultado del cobro, no el numero ni el codigo de
 * seguridad. Es el mismo criterio que aplica {@code usuarios} con la credencial hasheada — un
 * dato sensible no sale mas alla de donde hace falta.
 */
public record DatosDeTarjetaDTO(

        @NotBlank(message = "el numero de tarjeta es obligatorio")
        @Pattern(regexp = "\\d{13,19}", message = "el numero de tarjeta debe tener entre 13 y 19 digitos")
        String numero,

        @NotBlank(message = "el titular es obligatorio")
        String titular,

        @NotBlank(message = "el vencimiento es obligatorio")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "el vencimiento tiene formato MM/AA")
        String vencimiento,

        @NotBlank(message = "el codigo de seguridad es obligatorio")
        @Pattern(regexp = "\\d{3,4}", message = "el codigo de seguridad debe tener 3 o 4 digitos")
        String codigoSeguridad
) {
}
