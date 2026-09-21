package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.NativeLogPacketProducerProcessor;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
@ConditionalOnLocalOrTest
public class LocalNativeLogPacketProducer extends NativeLogPacketProducer {

    @Value("${routing.packet.bucket}")
    private String logPacketBucket;

    public LocalNativeLogPacketProducer(NativeLogPacketProducerProcessor producerProcessor) {
        super(producerProcessor);
    }

    @Override
    public void configure() {
        configureProducer(logPacketBucket, this::prepareTransportHeaders);
    }

    void prepareTransportHeaders(Exchange exchange) {
        exchange.getMessage().setHeader(FILE_NAME, exchange.getMessage().getHeader(LOG_FILENAME));
    }
}
