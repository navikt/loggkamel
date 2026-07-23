package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;

//TODO: When adding other consumers: make this class take a Teknologi and route strings, make the consumers pass that in
public abstract class LogStreamErrorHandler extends RouteBuilder {

    public static final String ORIGINAL_FILENAME = "originalFilename";

    @Value("${routing.postgres.invalid-message}")
    protected String postgresInvalidMessageDestinationUri;

    // This is not always where the invalid message ends up; rather it is the place we call to get the invalid message to its destination
    // In GCP, this is the consumer bucket: we call it to tell it to copy its file to the invalid message URI
    @Value("${routing.postgres.invalid-message-routing}")
    protected String postgresInvalidMessageRouting;

    @Autowired
    protected Metrics metrics;

    public abstract void configure();

    public void errorHandling() {
        // Use of the original message is allowed to enable local backout testing. Without stream caching this would
        // result in partially-read streams being sent to backout queues in GCP, if the GCP flow didn't drop message body
        // entirely and instead command GCP to copy the original file to the backout queue entirely outside of loggkamel
        getContext().setAllowUseOriginalMessage(true);
        getContext().setStreamCaching(false);

        onException(DependencyException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log(LoggingLevel.INFO, "Routing DependencyException to postgres invalid-messages channel after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .useOriginalBody() //testing
                .process(exchange -> {
                    metrics.incrementBackoutQueueMetrics(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL);
                })
                .process(exchange -> {
                    prepareExchangeForGCPCopyToInvalidMessageDestination(exchange, postgresInvalidMessageRouting, postgresInvalidMessageDestinationUri);
                })
                .to(postgresInvalidMessageRouting);

        onException(InvalidLogException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log(LoggingLevel.INFO, "Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody() //testing
                .process(exchange -> {
                    metrics.incrementBackoutQueueMetrics(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL);
                })
                .process(exchange -> {
                    prepareExchangeForGCPCopyToInvalidMessageDestination(exchange, postgresInvalidMessageRouting, postgresInvalidMessageDestinationUri);
                })
                .to(postgresInvalidMessageRouting);

        onException(Exception.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log(LoggingLevel.WARN, "Routing unhandled exception to postgres invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .log(LoggingLevel.DEBUG, "Exception stack trace: ${exception.stacktrace}")
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody() //testing
                .process(exchange -> {
                    metrics.incrementBackoutQueueMetrics(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL);
                })
                .process(exchange -> {
                    prepareExchangeForGCPCopyToInvalidMessageDestination(exchange, postgresInvalidMessageRouting, postgresInvalidMessageDestinationUri);
                })
                .to(postgresInvalidMessageRouting);
    }

    private void prepareExchangeForGCPCopyToInvalidMessageDestination(Exchange exchange, String originBucket, String destinationBucket) {
        if (originBucket.startsWith("google-storage://")) {
            exchange.getMessage().setHeader(GoogleCloudStorageConstants.OPERATION, GoogleCloudStorageOperations.copyObject);
            exchange.getMessage().setHeader(GoogleCloudStorageConstants.OBJECT_NAME, exchange.getMessage().getHeader(ORIGINAL_FILENAME));
            exchange.getMessage().setHeader(GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, destinationBucket);
            exchange.getMessage().setHeader(GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, exchange.getMessage().getHeader(ORIGINAL_FILENAME));
            exchange.getMessage().setBody(null); // Clear body contents since the GCP flow does not use them to move messages to the backout queue
        }
    }
}
