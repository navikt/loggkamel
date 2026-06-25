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

public abstract class LoggLineErrorHandler extends RouteBuilder {

    @Value("${routing.loggline.invalid-message}")
    protected String invalidMessageUri;

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
                .useOriginalBody()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .log("Routing DependencyException to invalid-messages channel after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class), Metrics.BackoutQueueType.deadletter))
                .to(invalidMessageUri);

        onException(InvalidLogException.class)
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .log("Routing InvalidLogException to invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .process(exchange -> metrics.incrementUnhappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class), Metrics.BackoutQueueType.invalid))
                .to(invalidMessageUri);

        onException(Exception.class)
                .useOriginalBody()
                .maximumRedeliveries(0)
                .handled(true)
                .log(LoggingLevel.WARN, "Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .process(exchange -> {
                    TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class) != null ? exchange.getVariable(TEKNOLOGI, TeknologiEnum.class) : TeknologiEnum.UNKNOWN;
                    metrics.incrementUnhappyPath(Metrics.Multiplicity.single, teknologi, Metrics.BackoutQueueType.invalid);
                })
                .to(invalidMessageUri);
    }
}
