package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.component.file.FileConstants.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

public abstract class LoggLineErrorHandler extends RouteBuilder {

    @Value("${routing.loggline.dead-letter.write}")
    protected String deadLetterUri;

    @Value("${routing.loggline.dead-letter.prefix}")
    protected String deadLetterPrefix;

    @Value("${routing.loggline.invalid-message.write}")
    protected String invalidMessageUri;

    @Value("${routing.loggline.invalid-message.prefix}")
    protected String invalidMessagePrefix;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Metrics metrics;

    public abstract void configure();

    public void errorHandling() {
        // Allows use of original message in exception handlers for cases where the message is an InputStream, as happens with GCP buckets
        getContext().setStreamCaching(true);
        getContext().setAllowUseOriginalMessage(true);

        onException(DependencyException.class)
                .log("Routing DependencyException to dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .process(exchange -> {
                    // Set directory prefix as part of the GCP filename, so that the file is written to the right target directory
                    exchange.getIn().setHeader(OBJECT_NAME, deadLetterPrefix + exchange.getIn().getHeader(FILE_NAME, String.class));
                })
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, String.class).toLowerCase(), Metrics.BackoutQueueType.deadletter))
                .to(deadLetterUri);

        onException(InvalidLogException.class)
                .log("Routing InvalidLogException to invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    // Set directory prefix as part of the GCP filename, so that the file is written to the right target directory
                    exchange.getIn().setHeader(OBJECT_NAME, invalidMessagePrefix + exchange.getIn().getHeader(FILE_NAME, String.class));
                })
                //DEBUG
                .process(exchange -> {
                    log.info("Message being sent to invalid log line directory");
                    log.info("message headers: " + exchange.getIn().getHeaders().toString());
                    log.info("message body: " + exchange.getIn().getBody().toString());
                    log.info("message destination: " + invalidMessageUri);
                })
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, String.class).toLowerCase(), Metrics.BackoutQueueType.invalid))
                .to(invalidMessageUri);

        onException(Exception.class)
                .log(LoggingLevel.WARN, "Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    // Set directory prefix as part of the GCP filename, so that the file is written to the right target directory
                    exchange.getIn().setHeader(OBJECT_NAME, invalidMessagePrefix + exchange.getIn().getHeader(FILE_NAME, String.class));
                })
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, "unhandled", Metrics.BackoutQueueType.invalid))
                .to(invalidMessageUri);
    }
}
