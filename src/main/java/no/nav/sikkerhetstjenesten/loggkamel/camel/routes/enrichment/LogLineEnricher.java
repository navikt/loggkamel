package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggLineErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.PostgresLogLineEnricher.POSTGRES_LOG_ENRICH_ROUTE;

@Component
public class LogLineEnricher extends LoggLineErrorHandler {

    public static String LOG_LINE_ENRICHER_ID = "generic-log-line-enricher";
    public static String LOG_LINE_ENRICHER_ROUTE = "direct:" + LOG_LINE_ENRICHER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_LINE_ENRICHER_ROUTE)
                .routeId(LOG_LINE_ENRICHER_ID)
                .log(LoggingLevel.INFO, "Determining which teknologi-specific enricher to use for ${header.CamelFileName}")
                .choice()
                    .when(variable(TEKNOLOGI).isEqualTo(TeknologiEnum.POSTGRESQL))
                        .log(LoggingLevel.INFO, "Routing log message ${header.CamelFileName} with teknologi ${variable.Teknologi} to Postgres enricher")
                        .to(POSTGRES_LOG_ENRICH_ROUTE)
                    .otherwise()
                        .log(LoggingLevel.WARN, "No specific enricher found for teknologi ${variable.Teknologi} in file ${header.CamelFileName}, sending to invalid message queue")
                        .throwException(new InvalidLogLineException("Could not determine which enricher to use for log message ${header.CamelFileName} with teknologi ${variable.Teknologi}"));
    }
}
