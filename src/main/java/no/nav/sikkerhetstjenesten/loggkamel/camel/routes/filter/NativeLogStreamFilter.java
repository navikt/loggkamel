package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter.NativeLogStreamFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter.NativeLogStreamSplitter.NATIVE_LOG_STREAM_SPLITTER_ROUTE;

@Component
public class NativeLogStreamFilter extends LogStreamErrorHandler {

    public static String NATIVE_LOG_STREAM_FILTER_ID = "native-log-stream-filter";
    public static String NATIVE_LOG_STREAM_FILTER_ROUTE = "direct:" + NATIVE_LOG_STREAM_FILTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(NATIVE_LOG_STREAM_FILTER_ROUTE)
                .routeId(NATIVE_LOG_STREAM_FILTER_ID)
                .log(LoggingLevel.DEBUG, "Determining whether to filter log message group ${header.CamelFileName}")
                .filter().method(NativeLogStreamFilterProcessor.class)
                .to(NATIVE_LOG_STREAM_SPLITTER_ROUTE);
    }
}
