package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogGroupEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.LogGroupFilter.LOG_GROUP_FILTER_ROUTE;

@Component
public class LogGroupEnricher extends SharedRouteErrorHandler  {

    public static String LOG_GROUP_ENRICHER_ID = "log-group-enricher";
    public static String LOG_GROUP_ENRICHER_ROUTE = "direct:" + LOG_GROUP_ENRICHER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_GROUP_ENRICHER_ROUTE)
                .routeId(LOG_GROUP_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Enriching Log-Group level attributes for ${header.CamelFileName}")
                .bean(LogGroupEnrichmentProcessor.class, "enrich")
                .to(LOG_GROUP_FILTER_ROUTE);
    }
}
