package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.STANDARDIZED_LOG_LINE_FILTER_ROUTE;

@Component
public class PostgresLogLineEnricher extends LogPacketErrorHandler {

    public static String POSTGRES_LOG_LINE_ENRICHER_ID = "postgres-log-line-enricher";
    public static String POSTGRES_LOG_LINE_ENRICHER_ROUTE = "direct:" + POSTGRES_LOG_LINE_ENRICHER_ID;

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(POSTGRES_LOG_LINE_ENRICHER_ROUTE)
                .routeId(POSTGRES_LOG_LINE_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Enriching log line from ${header.CamelFileName} line ${variable.PlaceInPacket}")
                .bean(PostgresLogLineEnrichmentProcessor.class, "enrich")
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(STANDARDIZED_LOG_LINE_FILTER_ROUTE);
    }
}
