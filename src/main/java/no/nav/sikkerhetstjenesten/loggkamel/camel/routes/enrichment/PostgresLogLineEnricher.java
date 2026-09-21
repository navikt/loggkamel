package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class PostgresLogLineEnricher extends LogPacketErrorHandler {

    private final PostgresLogLineEnrichmentProcessor enrichmentProcessor;

    public static final String POSTGRES_LOG_LINE_ENRICHER_ID = "postgres-log-line-enricher";
    public static final String POSTGRES_LOG_LINE_ENRICHER_ROUTE = "direct:" + POSTGRES_LOG_LINE_ENRICHER_ID;

    public PostgresLogLineEnricher(PostgresLogLineEnrichmentProcessor enrichmentProcessor) {
        this.enrichmentProcessor = enrichmentProcessor;
    }

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(POSTGRES_LOG_LINE_ENRICHER_ROUTE)
                .routeId(POSTGRES_LOG_LINE_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Enriching postgres log line from ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(enrichmentProcessor::enrich)
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}");
    }
}
