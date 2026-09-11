package com.passly.productoras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de una productora.
 *
 * <p><b>No lleva el id del creador.</b> Quien crea la productora es <i>identidad</i>, y la
 * identidad no la declara el cliente: viaja aparte (hoy en el header {@code X-Usuario-Id}, con
 * Spring Security en el principal autenticado). Si estuviera en este record, cualquiera podria dar
 * de alta una productora a nombre de otro.
 */
public record CrearProductoraRequest(

        @NotBlank(message = "el nombre comercial es obligatorio")
        @Size(max = 150, message = "el nombre comercial no puede superar los 150 caracteres")
        String nombreComercial,

        /*
         * Opcional: una productora puede operar antes de tener los datos fiscales cargados. El
         * patron acepta solo los 11 digitos, sin guiones, para no guardar el mismo CUIT con dos
         * formatos distintos y romper la unicidad.
         */
        @Pattern(regexp = "\\d{11}", message = "el cuit son 11 digitos sin guiones")
        String cuit,

        @Size(max = 2000, message = "la descripcion no puede superar los 2000 caracteres")
        String descripcion,

        @Size(max = 500, message = "la url del logo no puede superar los 500 caracteres")
        String logoUrl
) {
}
