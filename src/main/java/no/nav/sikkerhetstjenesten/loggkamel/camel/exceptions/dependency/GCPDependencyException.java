package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency;

public class GCPDependencyException extends DependencyException {

    public GCPDependencyException(String message) {
        super(message);
    }

    public GCPDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
