package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidLogException extends RuntimeException {

    public InvalidLogException(String message) {
        super(message);
    }

    public InvalidLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
