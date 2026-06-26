package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;

public abstract class LoggGroupErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.invalid-message}")
    protected String postgresInvalidMessageUri;

    @Value("${routing.fallback.invalid-message}")
    protected String fallbackInvalidMessageUri;

    @Autowired
    protected Metrics metrics;

    public abstract void configure();

    public void errorHandling() {
        // Allows use of original message in exception handlers for cases where the message is an InputStream, as happens with GCP buckets
        getContext().setStreamCaching(true);
        getContext().setAllowUseOriginalMessage(true);

        onException(DependencyException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log("Routing DependencyException to postgres invalid-messages channel after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.BackoutQueueType.deadletter);
                })
                .to(postgresInvalidMessageUri);

        // Other teknologi-specific dead letter queues go here

        onException(InvalidLogException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log("Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.BackoutQueueType.invalid);
                })
                .to(postgresInvalidMessageUri);

        // Other teknologi-specific invalid message queues go here

        onException(Exception.class)
                .log(LoggingLevel.WARN, "Routing unhandled exception to fallback invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .log(LoggingLevel.DEBUG, "Exception stack trace: ${exception.stacktrace}")
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.UNKNOWN, Metrics.BackoutQueueType.invalid);
                })
                .to(fallbackInvalidMessageUri);
    }
}
