package no.nav.sikkerhetstjenesten.loggkamel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.processor.LogFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.producer.LogProducer.POSTGRES_LOG_PRODUCER_ROUTE;

@Component
public class LogFilter extends SharedRouteErrorHandler {

    public static String POSTGRES_LOG_FILTER_ID = "postgres-log-filter";
    public static String POSTGRES_LOG_FILTER_ROUTE = "direct:" + POSTGRES_LOG_FILTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_FILTER_ROUTE)
                .routeId(POSTGRES_LOG_FILTER_ID)
                .log(LoggingLevel.INFO, "Determining whether to filter log message ${header.CamelFileName}")
                .log(LoggingLevel.DEBUG, "Message: ${body}, Headers: ${headers}")
                .bean(LogFilterProcessor.class, "doSomething")
                .log(LoggingLevel.DEBUG, "Per-message variables visible in the route after bean execution: ${variables}")
                .to(POSTGRES_LOG_PRODUCER_ROUTE);
    }
}
