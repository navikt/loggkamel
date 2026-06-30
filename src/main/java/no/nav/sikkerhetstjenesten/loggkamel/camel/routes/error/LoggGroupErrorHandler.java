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

//TODO: make this class take a Teknologi and route strings, make the consumers pass that in
public abstract class LoggGroupErrorHandler extends RouteBuilder {

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
//        getContext().setAllowUseOriginalMessage(true);

        onException(DependencyException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log(LoggingLevel.INFO, "Routing DependencyException to postgres invalid-messages channel after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
//                .useOriginalBody()
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.BackoutQueueType.deadletter);
                })
                .process(exchange -> {
                    prepareExchangeForGCPDelete(exchange, postgresInvalidMessageDestinationUri);
                })
                .to(postgresInvalidMessageRouting);

        onException(InvalidLogException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log(LoggingLevel.INFO, "Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .maximumRedeliveries(0)
                .handled(true)
//                .useOriginalBody()
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.BackoutQueueType.invalid);
                })
                .process(exchange -> {
                    prepareExchangeForGCPDelete(exchange, postgresInvalidMessageDestinationUri);
                })
                .to(postgresInvalidMessageRouting);

        onException(Exception.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log(LoggingLevel.WARN, "Routing unhandled exception to postgres invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .log(LoggingLevel.DEBUG, "Exception stack trace: ${exception.stacktrace}")
                .maximumRedeliveries(0)
                .handled(true)
//                .useOriginalBody()
                .process(exchange -> {
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.BackoutQueueType.invalid);
                })
                .process(exchange -> {
                    prepareExchangeForGCPDelete(exchange, postgresInvalidMessageDestinationUri);
                })
                .to(postgresInvalidMessageRouting);
    }

    private void prepareExchangeForGCPDelete(Exchange exchange, String destinationBucket) {
        if (postgresInvalidMessageRouting.startsWith("google-storage://")) {
            exchange.getIn().setHeader(GoogleCloudStorageConstants.OPERATION, GoogleCloudStorageOperations.copyObject);
            exchange.getIn().setHeader(GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, destinationBucket);
            exchange.getIn().setHeader(GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, exchange.getIn().getHeader(ORIGINAL_FILENAME));
        }
    }
}
