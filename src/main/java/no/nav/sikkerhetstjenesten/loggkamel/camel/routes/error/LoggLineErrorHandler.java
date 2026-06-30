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

public abstract class LoggLineErrorHandler extends RouteBuilder {

    @Value("${routing.loggline.invalid-message}")
    protected String invalidMessageUri;

    // This is not always where the invalid message ends up; rather it is the place we call to get the invalid message to its destination
    // In GCP, this is the consumer bucket: we call it to tell it to copy its file to the invalid message URI
    @Value("${routing.loggline.invalid-message-routing}")
    protected String invalidMessageRouting;

    @Autowired
    protected Metrics metrics;

    public abstract void configure();

    public void errorHandling() {
        getContext().setAllowUseOriginalMessage(true);

        onException(DependencyException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .useOriginalBody()
                .log(LoggingLevel.INFO, "Routing DependencyException to invalid-messages channel after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class), Metrics.BackoutQueueType.deadletter))
                .process(this::prepareExchangeForGCPDelete)
                .to(invalidMessageRouting);

        onException(InvalidLogException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody()
                .log(LoggingLevel.INFO, "Routing InvalidLogException to invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class), Metrics.BackoutQueueType.invalid))
                .process(this::prepareExchangeForGCPDelete)
                .to(invalidMessageRouting);

        onException(Exception.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody()
                .log(LoggingLevel.WARN, "Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .process(exchange -> {
                    TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class) != null ? exchange.getVariable(TEKNOLOGI, TeknologiEnum.class) : TeknologiEnum.UNKNOWN;
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.single, teknologi, Metrics.BackoutQueueType.invalid);
                })
                .process(this::prepareExchangeForGCPDelete)
                .to(invalidMessageRouting);
    }

    private void prepareExchangeForGCPDelete(Exchange exchange) {
        if (invalidMessageRouting.startsWith("google-storage://")) {
            exchange.getIn().setHeader(GoogleCloudStorageConstants.OPERATION, GoogleCloudStorageOperations.copyObject);
            exchange.getIn().setHeader(GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, invalidMessageUri);
            exchange.getIn().setHeader(GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, exchange.getIn().getHeader(GoogleCloudStorageConstants.OBJECT_NAME));
        }
    }
}
