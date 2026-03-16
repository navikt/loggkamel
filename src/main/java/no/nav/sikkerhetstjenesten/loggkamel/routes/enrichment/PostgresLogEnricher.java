package no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.processor.PostgresLogEnrichmentProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.producer.LogProducer.POSTGRES_LOG_PRODUCER_ROUTE;

@Component
public class PostgresLogEnricher extends RouteBuilder {

    public static String POSTGRES_LOG_ENRICH_ID = "postgres-log-enrich";
    public static String POSTGRES_LOG_ENRICH_ROUTE = "direct:" + POSTGRES_LOG_ENRICH_ID;

    @Override
    public void configure() {
        from(POSTGRES_LOG_ENRICH_ROUTE)
                .routeId(POSTGRES_LOG_ENRICH_ID)
                .log(LoggingLevel.INFO, "Message: ${body}, Headers: ${headers}")
                .bean(PostgresLogEnrichmentProcessor.class, "extract")
                //TODO: remove or update logging level for output logging
                .log(LoggingLevel.INFO, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(POSTGRES_LOG_PRODUCER_ROUTE);
    }
}
