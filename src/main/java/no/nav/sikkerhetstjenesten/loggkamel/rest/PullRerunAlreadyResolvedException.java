package no.nav.sikkerhetstjenesten.loggkamel.rest;

public class PullRerunAlreadyResolvedException extends RuntimeException {
    public PullRerunAlreadyResolvedException(String message) {
        super(message);
    }
}
