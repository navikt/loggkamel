package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.NativeLogPacketProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NativeLogPacketProducer extends LogStreamErrorHandler {

    @Value("${routing.packet.bucket}")
    String logPacketBucket;

    public static String NATIVE_LOG_PACKET_PRODUCER = "native-log-packet-producer";
    public static String NATIVE_LOG_PACKET_PRODUCER_ROUTE = "direct:" + NATIVE_LOG_PACKET_PRODUCER;

    @Override
    public void configure() {
        super.errorHandling();

        from(NATIVE_LOG_PACKET_PRODUCER_ROUTE)
                .routeId(NATIVE_LOG_PACKET_PRODUCER)
                .log(LoggingLevel.INFO, "Producing log packet ${header.CamelFileName} to log packet endpoint")
                .bean(NativeLogPacketProducerProcessor.class, "incrementMetrics")
                .bean(NativeLogPacketProducerProcessor.class, "mapToAuditloggLineMessageList")
                .toD(logPacketBucket);
    }
}
