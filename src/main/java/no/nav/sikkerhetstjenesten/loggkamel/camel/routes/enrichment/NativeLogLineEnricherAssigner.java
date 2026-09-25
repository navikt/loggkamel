package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.DB2LogLineEnricher.DB2_LOG_LINE_ENRICHER_ROUTE;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.PostgresLogLineEnricher.POSTGRES_LOG_LINE_ENRICHER_ROUTE;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.STANDARDIZED_LOG_LINE_FILTER_ROUTE;

@Component
public class NativeLogLineEnricherAssigner extends RouteBuilder {

    public static final String NATIVE_LOG_LINE_ENRICHER_ASSIGNER_ID = "native-log-line-enricher-assigner";
    public static final String NATIVE_LOG_LINE_ENRICHER_ROUTE = "direct:" + NATIVE_LOG_LINE_ENRICHER_ASSIGNER_ID;

    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public NativeLogLineEnricherAssigner(RouteConfigurationIdResolver routeConfigurationIdResolver) {
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(NATIVE_LOG_LINE_ENRICHER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.PACKET))
                .routeId(NATIVE_LOG_LINE_ENRICHER_ASSIGNER_ID)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.line))
                .log(LoggingLevel.DEBUG, "Determining which teknologi-specific enricher to use for ${header.LoggkamelFilename}")
                .choice()
                    .when(variable(TEKNOLOGI).isEqualTo(TeknologiEnum.POSTGRESQL))
                        .log(LoggingLevel.DEBUG, "Routing log message ${header.LoggkamelFilename} with teknologi ${variable.Teknologi} to Postgres enricher")
                        .to(POSTGRES_LOG_LINE_ENRICHER_ROUTE)
                    .when(variable(TEKNOLOGI).isEqualTo(TeknologiEnum.DB2))
                        .log(LoggingLevel.DEBUG, "Routing log message ${header.LoggkamelFilename} with teknologi ${variable.Teknologi} to DB2 enricher")
                        .to(DB2_LOG_LINE_ENRICHER_ROUTE)
                    .otherwise()
                        .log(LoggingLevel.WARN, "No specific enricher found for teknologi ${variable.Teknologi} in file ${header.LoggkamelFilename} line ${variable.PlaceInPacket}, sending to invalid message queue")
                        .throwException(new InvalidLogLineException("Could not determine which enricher to use for log message ${header.LoggkamelFilename} line ${variable.PlaceInPacket} with teknologi ${variable.Teknologi}"))
                .end()
                .to(STANDARDIZED_LOG_LINE_FILTER_ROUTE);
    }
}
