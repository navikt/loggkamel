package no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.processor.PostgresLogEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.producer.LogProducer.POSTGRES_LOG_PRODUCER_ROUTE;

@Component
public class PostgresLogEnricher extends SharedRouteErrorHandler {

    public static String POSTGRES_LOG_ENRICH_ID = "postgres-log-enrich";
    public static String POSTGRES_LOG_ENRICH_ROUTE = "direct:" + POSTGRES_LOG_ENRICH_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_ENRICH_ROUTE)
                .routeId(POSTGRES_LOG_ENRICH_ID)
                .log(LoggingLevel.INFO, "Enriching log message ${header.CamelFileName}")
                .log(LoggingLevel.DEBUG, "Message: ${body}, Headers: ${headers}")
                .bean(PostgresLogEnrichmentProcessor.class, "extract")
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(POSTGRES_LOG_PRODUCER_ROUTE);
    }
}
