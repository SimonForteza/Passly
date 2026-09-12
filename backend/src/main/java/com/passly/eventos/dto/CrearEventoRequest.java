package com.passly.eventos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Solicitud de creacion de un evento, con sus tipos de entrada anidados.
 *
 * <p>Las restricciones de aca son de <b>forma</b> y las verifica la capa de presentacion con
 * {@code @Valid}. Las reglas de <b>negocio</b> (que un evento no se pueda publicar dos veces,
 * que necesite al menos un tipo de entrada con cupo) viven en la capa de negocio y en la
 * entidad, no aca.
 *
 * <p>{@code OffsetDateTime} y no {@code LocalDateTime}: la hora de un evento tiene zona, y
 * perderla obligaria a que cada cliente adivine cual era.
 *
 * <p><b>El {@code idProductora} si va en el request, y no es una contradiccion con que la identidad
 * viaje en el header.</b> Son dos cosas distintas: <i>quien opera</i> es identidad y la declara el
 * servidor, mientras que <i>a nombre de que productora se publica</i> es un dato de la fiesta que
 * elige quien la crea — igual que al crear un repositorio en GitHub se elige la organizacion. Una
 * persona puede pertenecer a varias productoras, asi que el dato no es derivable de la identidad. Lo
 * que si se valida siempre es que quien opera pueda gestionar esa productora.
 */
public record CrearEventoRequest(

        @NotNull(message = "el id de la productora organizadora es obligatorio")
        Long idProductora,

        @NotBlank(message = "el nombre del evento es obligatorio")
        @Size(max = 150, message = "el nombre no puede superar los 150 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "la fecha y hora son obligatorias")
        @Future(message = "la fecha del evento debe ser futura")
        OffsetDateTime fechaHora,

        @NotBlank(message = "el lugar es obligatorio")
        @Size(max = 200, message = "el lugar no puede superar los 200 caracteres")
        String lugar,

        @NotEmpty(message = "el evento debe tener al menos un tipo de entrada")
        @Valid
        List<CrearTipoEntradaRequest> tiposEntrada
) {
}
