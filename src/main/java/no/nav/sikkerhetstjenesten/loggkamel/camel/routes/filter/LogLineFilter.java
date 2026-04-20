package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter.LogLineFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.ArkivLoggProducer.ARKIVLOGG_PRODUCER_ROUTE;

@Component
public class LogLineFilter extends SharedRouteErrorHandler {

    public static String LOG_LINE_FILTER_ID = "log-line-filter";
    public static String LOG_LINE_FILTER_ROUTE = "direct:" + LOG_LINE_FILTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_LINE_FILTER_ROUTE)
                .routeId(LOG_LINE_FILTER_ID)
                .log(LoggingLevel.INFO, "Determining whether to filter log message ${header.CamelFileName}")
                .log(LoggingLevel.DEBUG, "Message: ${body}, Headers: ${headers}")
                .filter().method(LogLineFilterProcessor.class)
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(ARKIVLOGG_PRODUCER_ROUTE);
    }
}
