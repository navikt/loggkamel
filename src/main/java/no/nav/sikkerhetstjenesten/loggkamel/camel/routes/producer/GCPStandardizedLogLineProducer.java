package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPStandardizedLogLineProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class GCPStandardizedLogLineProducer extends StandardizedLogLineProducer {

    private final GCPStandardizedLogLineProducerProcessor producerProcessor;

    public GCPStandardizedLogLineProducer(GCPStandardizedLogLineProducerProcessor producerProcessor) {
        this.producerProcessor = producerProcessor;
    }

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE)
                .routeId(STANDARDIZED_LOG_LINE_PRODUCER_ID)
                .log(LoggingLevel.INFO, "Producing log message ${header.LoggkamelFilename} line ${variable.PlaceInPacket} to GCP Logging")
                .process(producerProcessor::incrementMetrics)
                .process(producerProcessor::writeToGcpLogging);
    }
}
