package no.nav.sikkerhetstjenesten.loggkamel.rest;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
