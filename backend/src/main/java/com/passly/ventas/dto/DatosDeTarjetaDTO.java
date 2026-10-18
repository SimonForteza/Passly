package com.passly.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de la tarjeta con la que se paga. Propio de {@code ventas} y no el de
 * {@code pagos.dto}, a proposito: ni el Port {@code PasarelaDePago} ni la API publica de este
 * modulo deben conocer la forma del DTO de otro componente -- el Port existe justamente para
 * que {@code VentaServiceImpl} nunca sepa que implementacion concreta cobra, y filtrar un tipo
 * de {@code pagos} en {@code VentaService} acoplaria el contrato publico de este modulo al de
 * otro. Las mismas cuatro validaciones se duplican aca; es el mismo costo que ya paga el resto
 * del sistema (p.ej. {@code ItemDeCarritoDTO} propio en vez de reusar {@code TipoEntrada} de
 * {@code eventos}).
 *
 * <p>Nunca se persiste, mismo criterio que {@code pagos.dto.DatosDeTarjetaDTO}.
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
