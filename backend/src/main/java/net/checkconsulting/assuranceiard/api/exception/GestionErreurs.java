package net.checkconsulting.assuranceiard.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ProblemDetail emailDejaUtilise(EmailDejaUtiliseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
