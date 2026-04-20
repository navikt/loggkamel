package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter.LogGroupFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter.LogGroupSplitter.LOG_GROUP_SPLITTER_ROUTE;

@Component
public class LogGroupFilter extends LoggGroupErrorHandler {

    public static String LOG_GROUP_FILTER_ID = "log-group-filter";
    public static String LOG_GROUP_FILTER_ROUTE = "direct:" + LOG_GROUP_FILTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_GROUP_FILTER_ROUTE)
                .routeId(LOG_GROUP_FILTER_ID)
                .log(LoggingLevel.INFO, "Determining whether to filter log message group ${header.CamelFileName}")
                .log(LoggingLevel.DEBUG, "Message: ${body}, Headers: ${headers}")
                .filter().method(LogGroupFilterProcessor.class)
                .to(LOG_GROUP_SPLITTER_ROUTE);
    }
}
