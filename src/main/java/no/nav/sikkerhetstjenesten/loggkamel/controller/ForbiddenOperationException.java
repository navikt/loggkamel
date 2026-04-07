package no.nav.sikkerhetstjenesten.loggkamel.controller;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
