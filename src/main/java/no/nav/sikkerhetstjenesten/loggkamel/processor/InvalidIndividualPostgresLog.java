package no.nav.sikkerhetstjenesten.loggkamel.processor;

public class InvalidIndividualPostgresLog extends RuntimeException {

    public InvalidIndividualPostgresLog(String message) {
        super(message);
    }

    public InvalidIndividualPostgresLog(String message, Throwable cause) {
        super(message, cause);
    }
}