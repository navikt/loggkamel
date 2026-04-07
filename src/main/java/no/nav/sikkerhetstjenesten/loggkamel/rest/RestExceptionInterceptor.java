package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Hidden
@RestControllerAdvice(basePackages = "no.nav.sikkerhetstjenesten.loggkamel.controller")
public class RestExceptionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionInterceptor.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return mapToInternalServerError(HttpStatus.BAD_REQUEST, exception, request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenOperationException(ForbiddenOperationException exception, HttpServletRequest request) {
        return mapToInternalServerError(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(UpdatingNonexistentTaskException.class)
    public ResponseEntity<ErrorResponse> handleUpdatingNonexistentTaskException(UpdatingNonexistentTaskException exception, HttpServletRequest request) {
        return mapToInternalServerError(HttpStatus.CONFLICT, exception, request);
    }

    private ResponseEntity<ErrorResponse> mapToInternalServerError(HttpStatus httpStatus, Exception exception, HttpServletRequest request) {
        log.error("REST request failed for path {}", request.getRequestURI(), exception);
        ErrorResponse errorResponse = new ErrorResponse(
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    public record ErrorResponse(int errorCode, String message, String path, Instant timestamp) {
    }
}

