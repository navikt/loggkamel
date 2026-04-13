package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidPostgresLogGroupException extends InvalidLogGroupException {

    public InvalidPostgresLogGroupException(String message) {
        super(message);
    }

    public InvalidPostgresLogGroupException(String message, Throwable cause) {
        super(message, cause);
    }
}
