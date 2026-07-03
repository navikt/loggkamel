package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPStandardizedLogLineProducerProcessor;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class GCPStandardizedLogLineProducer extends StandardizedLogLineProducer {

    @Override
    public void configure() {
        super.errorHandling();

        from(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE)
            .routeId(STANDARDIZED_LOG_LINE_PRODUCER_ID)
            .log(LoggingLevel.INFO, "Producing log message ${header.CamelFileName} to GCP Logging")
            .bean(GCPStandardizedLogLineProducerProcessor.class, "incrementMetrics")
            .bean(GCPStandardizedLogLineProducerProcessor.class, "writeToGcpLogging");
    }
}
