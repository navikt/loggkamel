package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.DB2LogLineEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class DB2LogLineEnricher extends LogPacketErrorHandler {

    private final DB2LogLineEnrichmentProcessor enrichmentProcessor;

    public static final String DB2_LOG_LINE_ENRICHER_ID = "db2-log-line-enricher";
    public static final String DB2_LOG_LINE_ENRICHER_ROUTE = "direct:" + DB2_LOG_LINE_ENRICHER_ID;

    public DB2LogLineEnricher(DB2LogLineEnrichmentProcessor enrichmentProcessor) {
        this.enrichmentProcessor = enrichmentProcessor;
    }

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(DB2_LOG_LINE_ENRICHER_ROUTE)
                .routeId(DB2_LOG_LINE_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Enriching DB2 log line from ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(enrichmentProcessor::enrich)
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}");
    }
}
