package no.nav.sikkerhetstjenesten.loggkamel.service;

public class AuditloggTaskNotFoundException extends RuntimeException {
    public AuditloggTaskNotFoundException(String message) {
        super(message);
    }
}
