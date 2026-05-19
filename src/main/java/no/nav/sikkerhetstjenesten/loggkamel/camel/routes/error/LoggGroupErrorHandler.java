package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.component.file.FileConstants.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

public abstract class LoggGroupErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.dead-letter.write}")
    protected String postgresDeadLetterUri;

    @Value("${routing.postgres.dead-letter.prefix}")
    protected String postgresDeadLetterPrefix;

    @Value("${routing.postgres.invalid-message.write}")
    protected String postgresInvalidMessageUri;

    @Value("${routing.postgres.invalid-message.prefix}")
    protected String postgresInvalidMessagePrefix;

    @Value("${routing.fallback.invalid-message.write}")
    protected String fallbackInvalidMessageUri;

    @Value("${routing.fallback.invalid-message.write}")
    protected String fallbackInvalidMessagePrefix;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Metrics metrics;

    public abstract void configure();

    public void errorHandling() {
        // Allows use of original message in exception handlers for cases where the message is an InputStream, as happens with GCP buckets
        getContext().setStreamCaching(true);
        getContext().setAllowUseOriginalMessage(true);

        onException(DependencyException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log("Routing DependencyException to postgres dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .process(exchange -> {
                    // Set directory prefix as part of the GCP filename, so that the file is written to the right target directory
                    exchange.getIn().setHeader(OBJECT_NAME, postgresDeadLetterPrefix + exchange.getIn().getHeader(FILE_NAME, String.class));
                })
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL.name().toLowerCase(), Metrics.BackoutQueueType.deadletter);
                })
                .to(postgresDeadLetterUri);

        // Other teknologi-specific dead letter queues go here

        onException(InvalidLogException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log("Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    // Set directory prefix as part of the GCP filename, so that the file is written to the right target directory
                    exchange.getIn().setHeader(OBJECT_NAME, postgresInvalidMessagePrefix + exchange.getIn().getHeader(FILE_NAME, String.class));
                })
                //DEBUG
                .process(exchange -> {
                    log.info("Message being sent to invalid log line directory");
                    log.info("message headers: " + exchange.getIn().getHeaders().toString());
                    log.info("message body: " + exchange.getIn().getBody().toString());
                    log.info("message destination: " + postgresInvalidMessageUri);
                })
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL.name().toLowerCase(), Metrics.BackoutQueueType.invalid);
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
                    // Set directory prefix as part of the GCP filename, so that the file is written to the right target directory
                    exchange.getIn().setHeader(OBJECT_NAME, fallbackInvalidMessagePrefix + exchange.getIn().getHeader(FILE_NAME, String.class));
                })
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, "fallback", Metrics.BackoutQueueType.invalid);
                })
                .to(fallbackInvalidMessageUri);
    }
}
