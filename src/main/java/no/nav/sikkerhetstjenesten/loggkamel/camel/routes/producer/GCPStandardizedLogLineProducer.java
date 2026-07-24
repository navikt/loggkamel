package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPStandardizedLogLineProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class GCPStandardizedLogLineProducer extends StandardizedLogLineProducer {

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        //TODO: set up feature flag control over producer route
        from(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE)
                .routeId(STANDARDIZED_LOG_LINE_PRODUCER_ID)
                .autoStartup(false)
                .log(LoggingLevel.INFO, "Producing log message ${header.CamelFileName} line ${variable.PlaceInPacket} to GCP Logging")
                .bean(GCPStandardizedLogLineProducerProcessor.class, "incrementMetrics")
                .bean(GCPStandardizedLogLineProducerProcessor.class, "writeToGcpLogging");
    }
}
