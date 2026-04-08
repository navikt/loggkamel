package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency;

public class DatabaseDependencyException extends DependencyException {

    public DatabaseDependencyException(String message) {
        super(message);
    }

    public DatabaseDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
