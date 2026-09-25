package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.NativeLogPacketProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;

public abstract class NativeLogPacketProducer extends RouteBuilder {

    private final NativeLogPacketProducerProcessor producerProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String LOG_PACKET_EXTENSION = ".packet";

    public static final String NATIVE_LOG_PACKET_PRODUCER = "native-log-packet-producer";
    public static final String NATIVE_LOG_PACKET_PRODUCER_ROUTE = "direct:" + NATIVE_LOG_PACKET_PRODUCER;

    protected NativeLogPacketProducer(
            NativeLogPacketProducerProcessor producerProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.producerProcessor = producerProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    protected void configureProducer(String logPacketBucket, Processor transportHeaderInitializer) {
        from(NATIVE_LOG_PACKET_PRODUCER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.STREAM))
                .routeId(NATIVE_LOG_PACKET_PRODUCER)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.stream))
                .log(LoggingLevel.INFO, "Producing log packet ${header.LoggkamelFilename} to log packet endpoint")
                .process(producerProcessor::incrementMetrics)
                .process(producerProcessor::mapToAuditloggLineMessageList)
                .process(transportHeaderInitializer)
                .to(logPacketBucket);
    }
}
