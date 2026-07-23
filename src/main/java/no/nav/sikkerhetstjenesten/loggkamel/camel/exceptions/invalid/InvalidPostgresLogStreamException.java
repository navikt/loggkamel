package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidPostgresLogStreamException extends InvalidLogStreamException {

    public InvalidPostgresLogStreamException(String message) {
        super(message);
    }

    public InvalidPostgresLogStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
