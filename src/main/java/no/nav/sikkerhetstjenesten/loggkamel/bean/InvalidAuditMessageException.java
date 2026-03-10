package no.nav.sikkerhetstjenesten.loggkamel.bean;

public class InvalidAuditMessageException extends RuntimeException {

    public InvalidAuditMessageException(String message) {
        super(message);
    }

    public InvalidAuditMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}