package no.nav.sikkerhetstjenesten.loggkamel.bean;

public class InvalidIndividualPostgresLog extends RuntimeException {

    public InvalidIndividualPostgresLog(String message) {
        super(message);
    }

    public InvalidIndividualPostgresLog(String message, Throwable cause) {
        super(message, cause);
    }
}