package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.NativeLogPacketProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;

public abstract class NativeLogPacketProducer extends LogStreamErrorHandler {

    private final NativeLogPacketProducerProcessor producerProcessor;

    public static final String LOG_PACKET_EXTENSION = ".packet";

    public static final String NATIVE_LOG_PACKET_PRODUCER = "native-log-packet-producer";
    public static final String NATIVE_LOG_PACKET_PRODUCER_ROUTE = "direct:" + NATIVE_LOG_PACKET_PRODUCER;

    protected NativeLogPacketProducer(NativeLogPacketProducerProcessor producerProcessor) {
        this.producerProcessor = producerProcessor;
    }

    protected void configureProducer(String logPacketBucket, Processor transportHeaderInitializer) {
        super.errorHandling();

        from(NATIVE_LOG_PACKET_PRODUCER_ROUTE)
                .routeId(NATIVE_LOG_PACKET_PRODUCER)
                .log(LoggingLevel.INFO, "Producing log packet ${header.LoggkamelFilename} to log packet endpoint")
                .process(producerProcessor::incrementMetrics)
                .process(producerProcessor::mapToAuditloggLineMessageList)
                .process(transportHeaderInitializer)
                .to(logPacketBucket);
    }
}
