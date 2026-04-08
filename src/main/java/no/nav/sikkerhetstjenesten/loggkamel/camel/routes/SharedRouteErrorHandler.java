package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;

public abstract class SharedRouteErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.dead-letter}")
    protected String deadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    protected String invalidMessageUri;

    public abstract void configure();

    public void errorHandling() {
        onException(DependencyException.class)
                .maximumRedeliveries(3)
                .handled(true)
                .useOriginalMessage()
                .log("Routing DependencyException to dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(deadLetterUri);

        onException(InvalidLogLineException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalMessage()
                .log("Routing InvalidLogLineException to invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);

        onException(Exception.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalMessage()
                .log("Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);
    }
}
