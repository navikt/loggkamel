package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid;

public class InvalidPostgresLogPacketException extends InvalidLogPacketException {

    public InvalidPostgresLogPacketException(String message) {
        super(message);
    }

    public InvalidPostgresLogPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}