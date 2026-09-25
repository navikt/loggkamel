package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPStandardizedLogLineProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;

@Component
@ConditionalOnGCP
public class GCPStandardizedLogLineProducer extends StandardizedLogLineProducer {

    private final GCPStandardizedLogLineProducerProcessor producerProcessor;

    public GCPStandardizedLogLineProducer(
            GCPStandardizedLogLineProducerProcessor producerProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        super(routeConfigurationIdResolver);
        this.producerProcessor = producerProcessor;
    }

    @Override
    public void configure() {
        from(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.PACKET))
                .routeId(STANDARDIZED_LOG_LINE_PRODUCER_ID)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.line))
                .log(LoggingLevel.INFO, "Producing log message ${header.LoggkamelFilename} line ${variable.PlaceInPacket} to GCP Logging")
                .process(producerProcessor::incrementMetrics)
                .process(producerProcessor::writeToGcpLogging);
    }
}
