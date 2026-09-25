package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;

@Component
public class PostgresLogLineEnricher extends RouteBuilder {

    private final PostgresLogLineEnrichmentProcessor enrichmentProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String POSTGRES_LOG_LINE_ENRICHER_ID = "postgres-log-line-enricher";
    public static final String POSTGRES_LOG_LINE_ENRICHER_ROUTE = "direct:" + POSTGRES_LOG_LINE_ENRICHER_ID;

    public PostgresLogLineEnricher(
            PostgresLogLineEnrichmentProcessor enrichmentProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.enrichmentProcessor = enrichmentProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(POSTGRES_LOG_LINE_ENRICHER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.PACKET))
                .routeId(POSTGRES_LOG_LINE_ENRICHER_ID)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.line))
                .log(LoggingLevel.INFO, "Enriching postgres log line from ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(enrichmentProcessor::enrich)
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}");
    }
}
