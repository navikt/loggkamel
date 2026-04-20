package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.teknologi;

public class PostgresException extends RuntimeException {

    public PostgresException(String message) {
        super(message);
    }

    public PostgresException(String message, Throwable cause) {
        super(message, cause);
    }
}
