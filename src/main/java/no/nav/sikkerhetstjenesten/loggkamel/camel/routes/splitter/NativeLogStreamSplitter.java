package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.NativeLogStreamSplitterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.ExpressionAdapter;
import org.springframework.stereotype.Component;

import java.util.function.Function;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.NativeLogPacketProducer.NATIVE_LOG_PACKET_PRODUCER_ROUTE;

@Component
public class NativeLogStreamSplitter extends RouteBuilder {

    private final NativeLogStreamSplitterProcessor splitterProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String NATIVE_LOG_STREAM_SPLITTER_ID = "native-log-stream-splitter";
    public static final String NATIVE_LOG_STREAM_SPLITTER_ROUTE = "direct:" + NATIVE_LOG_STREAM_SPLITTER_ID;

    public NativeLogStreamSplitter(
            NativeLogStreamSplitterProcessor splitterProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.splitterProcessor = splitterProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(NATIVE_LOG_STREAM_SPLITTER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.STREAM))
                .routeId(NATIVE_LOG_STREAM_SPLITTER_ID)
                .log(LoggingLevel.INFO, "Splitting log file ${header.LoggkamelFilename} into bounded-size message lists")
                .split(methodReferenceToExpressionConverter(splitterProcessor::groupIntoPackets))
                    .streaming()
                    .process(splitterProcessor::prepareLogPacketHeaders)
                    .to(NATIVE_LOG_PACKET_PRODUCER_ROUTE);
    }

    private Expression methodReferenceToExpressionConverter(Function<Exchange, ?> evaluator) {
        return new ExpressionAdapter() {
            @Override
            public Object evaluate(Exchange exchange) {
                return evaluator.apply(exchange);
            }
        };
    }
}
