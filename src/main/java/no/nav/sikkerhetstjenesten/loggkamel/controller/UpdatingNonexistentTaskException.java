package no.nav.sikkerhetstjenesten.loggkamel.controller;

public class UpdatingNonexistentTaskException extends RuntimeException {
    public UpdatingNonexistentTaskException(String message) {
        super(message);
    }
}
