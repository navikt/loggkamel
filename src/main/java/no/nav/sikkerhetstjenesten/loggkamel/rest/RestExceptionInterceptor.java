package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.MissingNaisTeamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Set;

@Hidden
@RestControllerAdvice(basePackages = "no.nav.sikkerhetstjenesten.loggkamel")
public class RestExceptionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionInterceptor.class);

    private static final Set<HttpStatus> UPSTREAM_FAULT_STATUSES = Set.of(
            HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.GATEWAY_TIMEOUT
    );

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return mapToErrorResponse(HttpStatus.BAD_REQUEST, exception, request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenOperationException(ForbiddenOperationException exception, HttpServletRequest request) {
        return mapToErrorResponse(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(UpdatingNonexistentTaskException.class)
    public ResponseEntity<ErrorResponse> handleUpdatingNonexistentTaskException(UpdatingNonexistentTaskException exception, HttpServletRequest request) {
        return mapToErrorResponse(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(MissingNaisTeamException.class)
    public ResponseEntity<ErrorResponse> handleMissingNaisTeamException(MissingNaisTeamException exception, HttpServletRequest request) {
        return mapToErrorResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(DependencyException.class)
    public ResponseEntity<ErrorResponse> handleDependencyException(DependencyException exception, HttpServletRequest request) {
        return mapToErrorResponse(HttpStatus.BAD_GATEWAY, exception, request);
    }

    private ResponseEntity<ErrorResponse> mapToErrorResponse(HttpStatus httpStatus, Exception exception, HttpServletRequest request) {
        if (httpStatus.is5xxServerError() && !UPSTREAM_FAULT_STATUSES.contains(httpStatus)) {
            log.error("REST request failed for path {}", request.getRequestURI(), exception);
        } else {
            log.warn("REST request failed for path {} with status {}", request.getRequestURI(), httpStatus.value(), exception);
        }
        ErrorResponse errorResponse = new ErrorResponse(
                httpStatus.value(),
                exception.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    public record ErrorResponse(int errorCode, String message, String path, Instant timestamp) {
    }
}

