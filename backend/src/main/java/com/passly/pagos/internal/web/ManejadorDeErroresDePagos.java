package com.passly.pagos.internal.web;

import com.passly.pagos.PagoRechazadoException;
import com.passly.pagos.PasarelaDePagoNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapea las excepciones de dominio de Pagos a respuestas HTTP usando {@link ProblemDetail} (RFC
 * 9457), mismo criterio que {@code eventos.internal.web.ManejadorDeErrores}.
 */
@RestControllerAdvice
class ManejadorDeErroresDePagos {

    /**
     * La pasarela proceso el pedido y lo rechazo: <b>402 Payment Required</b>, no un generico
     * 400 o 500. Es el codigo pensado exactamente para esto en el estandar HTTP.
     */
    @ExceptionHandler(PagoRechazadoException.class)
    ProblemDetail rechazado(PagoRechazadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    /**
     * La pasarela no respondio: <b>503 Service Unavailable</b>. El problema es un externo caido,
     * no un bug de Passly (CLAUDE.md 7, resiliencia).
     */
    @ExceptionHandler(PasarelaDePagoNoDisponibleException.class)
    ProblemDetail noDisponible(PasarelaDePagoNoDisponibleException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
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
