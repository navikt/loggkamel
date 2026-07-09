package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.PostgresLogLineEnricher.POSTGRES_LOG_LINE_ENRICHER_ROUTE;

@Component
public class NativeLogLineEnricherAssigner extends LogPacketErrorHandler {

    public static String NATIVE_LOG_LINE_ENRICHER_ASSIGNER_ID = "native-log-line-enricher-assigner";
    public static String NATIVE_LOG_LINE_ENRICHER_ROUTE = "direct:" + NATIVE_LOG_LINE_ENRICHER_ASSIGNER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(NATIVE_LOG_LINE_ENRICHER_ROUTE)
                .routeId(NATIVE_LOG_LINE_ENRICHER_ASSIGNER_ID)
                .log(LoggingLevel.DEBUG, "Determining which teknologi-specific enricher to use for ${header.CamelFileName}")
                .choice()
                    .when(variable(TEKNOLOGI).isEqualTo(TeknologiEnum.POSTGRESQL))
                        .log(LoggingLevel.DEBUG, "Routing log message ${header.CamelFileName} with teknologi ${variable.Teknologi} to Postgres enricher")
                        .to(POSTGRES_LOG_LINE_ENRICHER_ROUTE)
                    .otherwise()
                        .log(LoggingLevel.WARN, "No specific enricher found for teknologi ${variable.Teknologi} in file ${header.CamelFileName} line ${variable.PlaceInPacket}, sending to invalid message queue")
                        .throwException(new InvalidLogLineException("Could not determine which enricher to use for log message ${header.CamelFileName} line ${variable.PlaceInPacket} with teknologi ${variable.Teknologi}"));
    }
}
