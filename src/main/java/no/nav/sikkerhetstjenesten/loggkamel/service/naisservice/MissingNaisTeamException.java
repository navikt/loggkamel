package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

public class MissingNaisTeamException extends RuntimeException {
    public MissingNaisTeamException(String message) {
        super(message);
    }
}
