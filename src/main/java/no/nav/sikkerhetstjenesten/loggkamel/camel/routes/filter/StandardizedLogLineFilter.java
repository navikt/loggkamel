package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter.StandardizedLogLineFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.StandardizedLogLineProducer.STANDARDIZED_LOG_LINE_PRODUCER_ROUTE;

@Component
public class StandardizedLogLineFilter extends RouteBuilder {

    private final StandardizedLogLineFilterProcessor filterProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String STANDARDIZED_LOG_LINE_FILTER_ID = "standardized-log-line-filter";
    public static final String STANDARDIZED_LOG_LINE_FILTER_ROUTE = "direct:" + STANDARDIZED_LOG_LINE_FILTER_ID;

    public static final String MESSAGE_SHOULD_BE_SKIPPED = "MessageShouldBeSkipped";

    public StandardizedLogLineFilter(
            StandardizedLogLineFilterProcessor filterProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.filterProcessor = filterProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(STANDARDIZED_LOG_LINE_FILTER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.PACKET))
                .routeId(STANDARDIZED_LOG_LINE_FILTER_ID)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.line))
                .log(LoggingLevel.DEBUG, "Determining whether to filter log message ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .filter(filterProcessor::messageIsMissingImmediateSkipHeader)
                .filter(filterProcessor::shouldLineActionTypeBeLoggedForThisTask)
                .to(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE);
    }
}
