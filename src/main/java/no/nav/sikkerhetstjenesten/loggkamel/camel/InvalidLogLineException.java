package no.nav.sikkerhetstjenesten.loggkamel.camel;

public class InvalidLogLineException extends RuntimeException {

    public InvalidLogLineException(String message) {
        super(message);
    }

    public InvalidLogLineException(String message, Throwable cause) {
        super(message, cause);
    }
}
