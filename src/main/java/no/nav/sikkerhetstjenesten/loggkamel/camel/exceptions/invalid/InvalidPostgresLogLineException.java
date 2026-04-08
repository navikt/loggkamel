package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidPostgresLogLineException extends InvalidLogLineException {

    public InvalidPostgresLogLineException(String message) {
        super(message);
    }

    public InvalidPostgresLogLineException(String message, Throwable cause) {
        super(message, cause);
    }
}