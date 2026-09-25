package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.DB2LogLineEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;

@Component
public class DB2LogLineEnricher extends RouteBuilder {

    private final DB2LogLineEnrichmentProcessor enrichmentProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String DB2_LOG_LINE_ENRICHER_ID = "db2-log-line-enricher";
    public static final String DB2_LOG_LINE_ENRICHER_ROUTE = "direct:" + DB2_LOG_LINE_ENRICHER_ID;

    public DB2LogLineEnricher(
            DB2LogLineEnrichmentProcessor enrichmentProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.enrichmentProcessor = enrichmentProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(DB2_LOG_LINE_ENRICHER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.PACKET))
                .routeId(DB2_LOG_LINE_ENRICHER_ID)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.line))
                .log(LoggingLevel.INFO, "Enriching DB2 log line from ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(enrichmentProcessor::enrich)
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}");
    }
}
