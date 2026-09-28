package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.NativeLogPacketProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.CONTENT_TYPE;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class NativeLogPacketProducerTest {

    private static final String FILENAME = "packet.json";

    @Test
    void localProducerSetsOnlyCamelFilenameHeader() {
        Exchange exchange = exchangeWithFilename();
        LocalNativeLogPacketProducer producer = new LocalNativeLogPacketProducer(
                mock(NativeLogPacketProducerProcessor.class),
                mock(RouteConfigurationIdResolver.class)
        );

        producer.prepareTransportHeaders(exchange);

        assertEquals(FILENAME, exchange.getMessage().getHeader(FILE_NAME));
        assertNull(exchange.getMessage().getHeader(OBJECT_NAME));
        assertNull(exchange.getMessage().getHeader(CONTENT_TYPE));
    }

    @Test
    void gcpProducerSetsOnlyGoogleStorageHeaders() {
        Exchange exchange = exchangeWithFilename();
        GCPNativeLogPacketProducer producer = new GCPNativeLogPacketProducer(
                mock(NativeLogPacketProducerProcessor.class),
                mock(RouteConfigurationIdResolver.class)
        );

        producer.prepareTransportHeaders(exchange);

        assertEquals(FILENAME, exchange.getMessage().getHeader(OBJECT_NAME));
        assertEquals(APPLICATION_JSON.getMimeType(), exchange.getMessage().getHeader(CONTENT_TYPE));
        assertNull(exchange.getMessage().getHeader(FILE_NAME));
    }

    private Exchange exchangeWithFilename() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader(LOG_FILENAME, FILENAME);
        return exchange;
    }
}
