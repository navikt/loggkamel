package no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency;

public class EntraProxyDependencyException extends DependencyException {

    public EntraProxyDependencyException(String message) {
        super(message);
    }

    public EntraProxyDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
