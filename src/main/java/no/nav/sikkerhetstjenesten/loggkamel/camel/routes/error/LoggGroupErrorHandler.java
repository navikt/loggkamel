package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;

public abstract class LoggGroupErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.dead-letter}")
    protected String postgresDeadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    protected String postgresInvalidMessageUri;

    @Value("${routing.fallback.invalid-message}")
    protected String fallbackInvalidMessageUri;

    @Autowired
    protected ObjectMapper objectMapper;

    public abstract void configure();

    public void errorHandling() {
        onException(DependencyException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .useOriginalMessage()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .log("Routing DependencyException to postgres dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(postgresDeadLetterUri);

        // Other teknologi-specific dead letter queues go here

        onException(InvalidLogException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .useOriginalMessage()
                .maximumRedeliveries(0)
                .handled(true)
                .log("Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(postgresInvalidMessageUri);

        // Other teknologi-specific invalid message queues go here

        onException(Exception.class)
                .useOriginalMessage()
                .maximumRedeliveries(0)
                .handled(true)
                .log(LoggingLevel.WARN, "Routing unhandled exception to fallback invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(fallbackInvalidMessageUri);
    }
}
