package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidDB2LogLineException extends InvalidLogLineException {
    public InvalidDB2LogLineException(String message) {
        super(message);
    }

    public InvalidDB2LogLineException(String message, Throwable cause) {
        super(message, cause);
    }
}
