package com.passly.ventas.internal.web;

import com.passly.eventos.CupoInsuficienteException;
import com.passly.ventas.CarritoDeOtroCompradorException;
import com.passly.ventas.CarritoVacioException;
import com.passly.ventas.CarritoVencidoException;
import com.passly.ventas.OrdenNoEncontradaException;
import com.passly.ventas.PagoRechazadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapea las excepciones de dominio de Ventas a respuestas HTTP usando {@link ProblemDetail}
 * (RFC 9457).
 *
 * <p><b>Nombre con sufijo a proposito</b> (ver {@code ManejadorDeErroresDeProductoras}): ya
 * existe un {@code ManejadorDeErrores} generico en {@code eventos}.
 *
 * <p><b>No maneja {@code MethodArgumentNotValidException}</b>: los advices de eventos y usuarios
 * ya lo cubren globalmente.
 *
 * <p><b>Si maneja {@code CupoInsuficienteException}, de {@code eventos}, y
 * {@code ObjectOptimisticLockingFailureException}, de Spring.</b> Ninguna de las dos es de este
 * modulo, pero ambas solo pueden ocurrir en el flujo de confirmar una compra, y traducirlas aca
 * es mas simple que agregarle a Eventos un advice que solo Ventas necesitaria.
 */
@RestControllerAdvice
class ManejadorDeErroresDeVentas {

    @ExceptionHandler(CarritoVacioException.class)
    ProblemDetail carritoVacio(CarritoVacioException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(CarritoVencidoException.class)
    ProblemDetail carritoVencido(CarritoVencidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 409: no es que a quien pide le falte permiso — es que el carrito de esta sesion quedo
     * asociado a otro comprador (ver Javadoc de {@code CarritoDeCompra.verificarDueño}).
     */
    @ExceptionHandler(CarritoDeOtroCompradorException.class)
    ProblemDetail carritoDeOtroComprador(CarritoDeOtroCompradorException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** 402 Payment Required: el unico status pensado para esto en el estandar HTTP. */
    @ExceptionHandler(PagoRechazadoException.class)
    ProblemDetail pagoRechazado(PagoRechazadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(CupoInsuficienteException.class)
    ProblemDetail cupoInsuficiente(CupoInsuficienteException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Dos confirmaciones concurrentes chocaron en el @Version de un tipo de entrada: 409. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail bloqueoOptimista(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Otra compra modifico el mismo cupo al mismo tiempo; reintentar la confirmacion");
    }

    /**
     * 404 y no 403 a proposito (ver Javadoc de {@code OrdenNoEncontradaException}): a diferencia
     * de una productora, publica en una cartelera, una orden ajena no deberia siquiera confirmar
     * que existe.
     */
    @ExceptionHandler(OrdenNoEncontradaException.class)
    ProblemDetail ordenNoEncontrada(OrdenNoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
