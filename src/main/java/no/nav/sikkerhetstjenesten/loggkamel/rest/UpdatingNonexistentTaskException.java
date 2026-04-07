package no.nav.sikkerhetstjenesten.loggkamel.rest;

public class UpdatingNonexistentTaskException extends RuntimeException {
    public UpdatingNonexistentTaskException(String message) {
        super(message);
    }
}
