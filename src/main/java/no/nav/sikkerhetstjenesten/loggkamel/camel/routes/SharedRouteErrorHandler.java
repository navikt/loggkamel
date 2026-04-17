package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class SharedRouteErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.dead-letter}")
    protected String deadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    protected String invalidMessageUri;

    @Autowired
    protected ObjectMapper objectMapper;

    public abstract void configure();

    //TODO: currently this sends all errors to the postgres-specific channels, want technologi-specific channels instead
    //TODO: once have checkpointing of LogLine messages, useOriginalBody() on backout queues and remove body conversion processors
    public void errorHandling() {
        onException(DependencyException.class)
//                .useOriginalBody()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .process(exchange -> {
                    Object body = exchange.getMessage().getBody();
                    if (body instanceof AuditloggLineMessage msg) {
                        exchange.getMessage().setBody(objectMapper.writeValueAsString(msg));
                    } else {
                        exchange.getMessage().setBody(body != null ? body.toString() : "");
                    }
                })
                .log("Routing DependencyException to dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(deadLetterUri);

        onException(InvalidLogException.class)
//                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    Object body = exchange.getMessage().getBody();
                    if (body instanceof AuditloggLineMessage msg) {
                        exchange.getMessage().setBody(objectMapper.writeValueAsString(msg));
                    } else {
                        exchange.getMessage().setBody(body != null ? body.toString() : "");
                    }
                })
                .log("Routing InvalidLogException to invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);

        onException(Exception.class)
//                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    Object body = exchange.getMessage().getBody();
                    if (body instanceof AuditloggLineMessage msg) {
                        exchange.getMessage().setBody(objectMapper.writeValueAsString(msg));
                    } else {
                        exchange.getMessage().setBody(body != null ? body.toString() : "");
                    }
                })
                .log(LoggingLevel.WARN, "Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);
    }
}
