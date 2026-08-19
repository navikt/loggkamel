package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.DB2LogLineEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.STANDARDIZED_LOG_LINE_FILTER_ROUTE;

@Component
public class DB2LogLineEnricher extends LogPacketErrorHandler {

    public static final String DB2_LOG_LINE_ENRICHER_ID = "db2-log-line-enricher";
    public static final String DB2_LOG_LINE_ENRICHER_ROUTE = "direct:" + DB2_LOG_LINE_ENRICHER_ID;

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(DB2_LOG_LINE_ENRICHER_ROUTE)
                .routeId(DB2_LOG_LINE_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Enriching DB2 log line from ${header.CamelFileName} line ${variable.PlaceInPacket}")
                .bean(DB2LogLineEnrichmentProcessor.class, "enrich")
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(STANDARDIZED_LOG_LINE_FILTER_ROUTE);
    }
}
