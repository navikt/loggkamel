package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter.StandardizedLogLineFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.StandardizedLogLineProducer.STANDARDIZED_LOG_LINE_PRODUCER_ROUTE;

@Component
public class StandardizedLogLineFilter extends LogPacketErrorHandler {

    public static String STANDARDIZED_LOG_LINE_FILTER_ID = "standardized-log-line-filter";
    public static String STANDARDIZED_LOG_LINE_FILTER_ROUTE = "direct:" + STANDARDIZED_LOG_LINE_FILTER_ID;

    public static final String MESSAGE_SHOULD_BE_SKIPPED = "MessageShouldBeSkipped";

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(STANDARDIZED_LOG_LINE_FILTER_ROUTE)
                .routeId(STANDARDIZED_LOG_LINE_FILTER_ID)
                .log(LoggingLevel.DEBUG, "Determining whether to filter log message ${header.CamelFileName} line ${variable.PlaceInPacket}")
                .filter().method(StandardizedLogLineFilterProcessor.class, "messageIsMissingImmediateSkipHeader")
                .filter().method(StandardizedLogLineFilterProcessor.class, "doesLineActionMatchRelevantAuditloggArkiv")
                .to(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE);
    }
}
