package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency;

public class NaisDependencyException extends DependencyException {

    public NaisDependencyException(String message) {
        super(message);
    }

    public NaisDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
