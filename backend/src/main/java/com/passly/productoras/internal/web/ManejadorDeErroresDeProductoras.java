package com.passly.productoras.internal.web;

import com.passly.productoras.NoEsMiembroDeLaProductoraException;
import com.passly.productoras.NombreComercialYaRegistradoException;
import com.passly.productoras.ProductoraNoEncontradaException;
import com.passly.productoras.RolIncompatibleConLaMembresiaException;
import com.passly.productoras.YaEsMiembroDeLaProductoraException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapea las excepciones de dominio de Productoras a respuestas HTTP usando {@link ProblemDetail}
 * (RFC 9457).
 *
 * <p><b>Nombre con sufijo a proposito.</b> Spring deriva el nombre del bean del nombre simple de la
 * clase, y ya existen {@code ManejadorDeErrores} en {@code eventos} y
 * {@code ManejadorDeErroresDeUsuarios}: repetir el nombre romperia el arranque con
 * {@code ConflictingBeanDefinitionException}.
 *
 * <p><b>No maneja {@code MethodArgumentNotValidException}</b>: los advices de eventos y usuarios ya
 * lo cubren con identico comportamiento y son globales, asi que un tercer handler solo agregaria
 * ambiguedad sin cambiar la respuesta.
 */
@RestControllerAdvice
class ManejadorDeErroresDeProductoras {

    @ExceptionHandler(ProductoraNoEncontradaException.class)
    ProblemDetail productoraNoEncontrada(ProductoraNoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** El nombre comercial ya esta tomado: choca con un recurso existente, es 409. */
    @ExceptionHandler(NombreComercialYaRegistradoException.class)
    ProblemDetail nombreDuplicado(NombreComercialYaRegistradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(YaEsMiembroDeLaProductoraException.class)
    ProblemDetail miembroDuplicado(YaEsMiembroDeLaProductoraException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 409 y no 403: no es que a quien pide le falte permiso — la peticion en si es inconsistente,
     * porque el rol global del invitado no admite el rol interno que se le quiere dar.
     */
    @ExceptionHandler(RolIncompatibleConLaMembresiaException.class)
    ProblemDetail rolIncompatible(RolIncompatibleConLaMembresiaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 403 y no 404: el recurso existe y se sabe cual es, lo que falta es la autorizacion. Devolver
     * 404 para ocultar su existencia seria razonable en otros dominios, pero aca las productoras
     * estan en una cartelera publica: no hay nada que ocultar.
     */
    @ExceptionHandler(NoEsMiembroDeLaProductoraException.class)
    ProblemDetail noAutorizado(NoEsMiembroDeLaProductoraException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Falta el header con la identidad del actor.
     *
     * <p>Es <b>401 y no 400</b> a proposito: hoy el actor viaja en {@code X-Usuario-Id} y manana en
     * un token, pero en los dos casos lo que falta es la identidad, no un dato del formulario. Que
     * el status ya sea el definitivo significa que cuando PAS-6 reemplace el header por Spring
     * Security, el contrato HTTP no cambia para los clientes.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail faltaIdentidad(MissingRequestHeaderException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Falta la identidad de quien opera en el header " + ex.getHeaderName());
    }
}
