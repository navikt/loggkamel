package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidDB2LogPacketException extends InvalidLogPacketException {
    public InvalidDB2LogPacketException(String message) {
        super(message);
    }

    public InvalidDB2LogPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}
