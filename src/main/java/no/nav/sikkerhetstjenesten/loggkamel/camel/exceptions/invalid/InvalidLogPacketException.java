package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidLogPacketException extends InvalidLogException {

    public InvalidLogPacketException(String message) {
        super(message);
    }

    public InvalidLogPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}
