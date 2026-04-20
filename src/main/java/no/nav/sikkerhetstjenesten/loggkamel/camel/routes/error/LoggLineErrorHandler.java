package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class LoggLineErrorHandler extends RouteBuilder {

    @Value("${routing.loggline.dead-letter}")
    protected String deadLetterUri;

    @Value("${routing.loggline.invalid-message}")
    protected String invalidMessageUri;

    @Autowired
    protected ObjectMapper objectMapper;

    public abstract void configure();

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
