package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidLogGroupException extends InvalidLogException {

    public InvalidLogGroupException(String message) {
        super(message);
    }

    public InvalidLogGroupException(String message, Throwable cause) {
        super(message, cause);
    }
}
