package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidLogLineException extends InvalidLogException {

    public InvalidLogLineException(String message) {
        super(message);
    }

    public InvalidLogLineException(String message, Throwable cause) {
        super(message, cause);
    }
}
