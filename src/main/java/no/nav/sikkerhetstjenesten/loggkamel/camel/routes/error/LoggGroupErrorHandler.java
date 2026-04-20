package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.teknologi.PostgresException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class LoggGroupErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.dead-letter}")
    protected String deadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    protected String invalidMessageUri;

    @Autowired
    protected ObjectMapper objectMapper;

    public abstract void configure();

    //TODO: currently this sends all errors to the postgres-specific channels, want technologi-specific channels instead
    public void errorHandling() {
        onException(DependencyException.class)
                .useOriginalMessage()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .log("Routing DependencyException to dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(deadLetterUri);

        onException(InvalidLogException.class)
                .useOriginalMessage()
                .maximumRedeliveries(0)
                .handled(true)
                .log("Routing InvalidLogException to invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);

        onException(Exception.class)
                .useOriginalMessage()
                .maximumRedeliveries(0)
                .handled(true)
                .log(LoggingLevel.WARN, "Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);
    }
}
