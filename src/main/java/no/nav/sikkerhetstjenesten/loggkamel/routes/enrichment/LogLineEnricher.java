package no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.processor.InvalidIndividualPostgresLog;
import no.nav.sikkerhetstjenesten.loggkamel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.PostgresLogLineEnricher.POSTGRES_LOG_ENRICH_ROUTE;

@Component
public class LogLineEnricher extends SharedRouteErrorHandler {

    public static String LOG_LINE_ENRICHER_ID = "generic-log-line-enricher";
    public static String LOG_LINE_ENRICHER_ROUTE = "direct:" + LOG_LINE_ENRICHER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_LINE_ENRICHER_ROUTE)
                .routeId(LOG_LINE_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Determining which teknologi-specific enricher to use for ${header.CamelFileName}")
                .choice()
                    .when(exchangeProperty(TEKNOLOGI).isEqualTo(TeknologiEnum.POSTGRESQL))
                        .to(POSTGRES_LOG_ENRICH_ROUTE)
                    .otherwise()
                        .log(LoggingLevel.INFO, "No specific enricher found for teknologi ${header.TEKNOLOGI} in file ${header.CamelFileName}, sending to invalid message queue")
                        // TODO: more generic exception here
                        .throwException(new InvalidIndividualPostgresLog("Could not determine which enricher to use for log message ${header.CamelFileName} with teknologi ${header.TEKNOLOGI}"));
    }
}
