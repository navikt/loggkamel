package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.NativeLogPacketProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.CONTENT_TYPE;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Component
@ConditionalOnGCP
public class GCPNativeLogPacketProducer extends NativeLogPacketProducer {

    @Value("${routing.packet.bucket}")
    private String logPacketBucket;

    public GCPNativeLogPacketProducer(
            NativeLogPacketProducerProcessor producerProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        super(producerProcessor, routeConfigurationIdResolver);
    }

    @Override
    public void configure() {
        configureProducer(logPacketBucket, this::prepareTransportHeaders);
    }

    void prepareTransportHeaders(Exchange exchange) {
        exchange.getMessage().setHeader(OBJECT_NAME, exchange.getMessage().getHeader(LOG_FILENAME));
        exchange.getMessage().setHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
    }
}
