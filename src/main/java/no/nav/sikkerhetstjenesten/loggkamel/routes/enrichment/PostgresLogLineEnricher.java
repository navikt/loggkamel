package no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.PostgresLogEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.filter.LogLineFilter.LOG_LINE_FILTER_ROUTE;

@Component
public class PostgresLogLineEnricher extends SharedRouteErrorHandler {

    public static String POSTGRES_LOG_ENRICH_ID = "postgres-log-line-enricher";
    public static String POSTGRES_LOG_ENRICH_ROUTE = "direct:" + POSTGRES_LOG_ENRICH_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_ENRICH_ROUTE)
                .routeId(POSTGRES_LOG_ENRICH_ID)
                .log(LoggingLevel.INFO, "Enriching log message ${header.CamelFileName}")
                .log(LoggingLevel.DEBUG, "Message: ${body}, Headers: ${headers}")
                .bean(PostgresLogEnrichmentProcessor.class, "enrich")
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(LOG_LINE_FILTER_ROUTE);
    }
}
