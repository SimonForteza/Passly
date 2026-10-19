package com.passly.seguridad.internal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ProblemDetail;
@RestControllerAdvice
class ErroresHttp {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail responder(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(),error.getReason()==null?"No se pudo completar la operación.":error.getReason());
    }
}
