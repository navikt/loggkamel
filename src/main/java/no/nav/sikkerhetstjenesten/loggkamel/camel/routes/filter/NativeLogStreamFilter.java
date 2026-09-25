package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter.NativeLogStreamFilterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter.NativeLogStreamSplitter.NATIVE_LOG_STREAM_SPLITTER_ROUTE;

@Component
public class NativeLogStreamFilter extends RouteBuilder {

    private final NativeLogStreamFilterProcessor filterProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String NATIVE_LOG_STREAM_FILTER_ID = "native-log-stream-filter";
    public static final String NATIVE_LOG_STREAM_FILTER_ROUTE = "direct:" + NATIVE_LOG_STREAM_FILTER_ID;

    public NativeLogStreamFilter(
            NativeLogStreamFilterProcessor filterProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.filterProcessor = filterProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(NATIVE_LOG_STREAM_FILTER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.STREAM))
                .routeId(NATIVE_LOG_STREAM_FILTER_ID)
                .log(LoggingLevel.DEBUG, "Determining whether to filter log message group ${header.LoggkamelFilename}")
                .filter(filterProcessor::doesAuditloggTaskRequireForwardingLogs)
                .to(NATIVE_LOG_STREAM_SPLITTER_ROUTE);
    }
}
