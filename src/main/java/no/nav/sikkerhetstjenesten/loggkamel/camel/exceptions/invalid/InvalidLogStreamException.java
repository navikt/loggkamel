package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidLogStreamException extends InvalidLogException {

    public InvalidLogStreamException(String message) {
        super(message);
    }

    public InvalidLogStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
