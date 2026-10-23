package no.nav.sikkerhetstjenesten.loggkamel.service;

public class InvalidAuditloggTaskStateException extends RuntimeException {
    public InvalidAuditloggTaskStateException(String message) {
        super(message);
    }
}
