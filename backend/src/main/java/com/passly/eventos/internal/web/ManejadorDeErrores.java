package com.passly.eventos.internal.web;

import com.passly.eventos.EventoNoEncontradoException;
import com.passly.eventos.ProductoraNoGestionableException;
import com.passly.eventos.TipoEntradaNoEncontradoException;
import com.passly.eventos.TransicionDeEstadoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapea las excepciones de dominio a respuestas HTTP usando {@link ProblemDetail} (RFC 9457,
 * nativo de Spring desde Boot 3).
 *
 * <p>Sin esto la demo muestra stack traces; con esto, el <b>409</b> al intentar republicar un
 * evento es la evidencia mas visible de que hay una regla de negocio real detras del endpoint.
 */
@RestControllerAdvice
class ManejadorDeErrores {

    @ExceptionHandler(EventoNoEncontradoException.class)
    ProblemDetail evento(EventoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TipoEntradaNoEncontradoException.class)
    ProblemDetail tipoEntrada(TipoEntradaNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** El recurso existe pero su estado no admite la operacion: 409, no 404. */
    @ExceptionHandler(TransicionDeEstadoInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionDeEstadoInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Quien opera no gestiona esa productora: <b>403</b>, no 404.
     *
     * <p>Es el error que hace visible el aislamiento entre productoras, y por eso es el mas
     * importante de la demo: una productora no puede publicar la fiesta de otra. No se devuelve 404
     * para ocultar la existencia del evento porque los eventos estan en una cartelera publica.
     */
    @ExceptionHandler(ProductoraNoGestionableException.class)
    ProblemDetail productoraNoGestionable(ProductoraNoGestionableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Falta el header con la identidad del actor: <b>401</b>, no 400.
     *
     * <p>Lo que falta es la identidad, no un campo del formulario. Que el status ya sea el
     * definitivo significa que cuando PAS-6 reemplace el header por Spring Security, el contrato
     * HTTP no cambia para los clientes.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail faltaIdentidad(MissingRequestHeaderException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Falta la identidad de quien opera en el header " + ex.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o mas campos no son validos");

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errores.put(error.getField(), error.getDefaultMessage()));
        detalle.setProperty("errores", errores);

        return detalle;
    }
}
