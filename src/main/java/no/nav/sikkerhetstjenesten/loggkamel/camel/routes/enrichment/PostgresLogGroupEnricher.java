package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogGroupEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.LogGroupFilter.LOG_GROUP_FILTER_ROUTE;

@Component
public class PostgresLogGroupEnricher extends SharedRouteErrorHandler  {

    public static String POSTGRES_LOG_GROUP_ENRICHER_ID = "postgres-log-group-enricher";
    public static String POSTGRES_LOG_GROUP_ENRICHER_ROUTE = "direct:" + POSTGRES_LOG_GROUP_ENRICHER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_GROUP_ENRICHER_ROUTE)
                .routeId(POSTGRES_LOG_GROUP_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Enriching Log-Group level attributes for ${header.CamelFileName}")
                .bean(PostgresLogGroupEnrichmentProcessor.class, "enrich")
                .to(LOG_GROUP_FILTER_ROUTE);
    }
}
