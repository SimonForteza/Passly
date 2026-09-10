package com.passly.usuarios.internal.web;

import com.passly.usuarios.EmailYaRegistradoException;
import com.passly.usuarios.UsuarioNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapea las excepciones de dominio de Usuarios a respuestas HTTP usando {@link ProblemDetail}
 * (RFC 9457, nativo de Spring desde Boot 3).
 *
 * <p><b>Nombre con sufijo a proposito.</b> Spring deriva el nombre del bean del nombre simple de
 * la clase; {@code eventos} ya tiene un {@code ManejadorDeErrores}, y repetir el nombre romperia
 * el arranque con {@code ConflictingBeanDefinitionException}. Cada modulo trae su propio advice
 * para quedar self-contained; los handlers de dominio (404/409) no se solapan entre modulos.
 */
@RestControllerAdvice
class ManejadorDeErroresDeUsuarios {

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ProblemDetail usuario(UsuarioNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** El email ya esta tomado: choca con un recurso existente, es 409, no 400. */
    @ExceptionHandler(EmailYaRegistradoException.class)
    ProblemDetail emailDuplicado(EmailYaRegistradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
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
