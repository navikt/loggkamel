package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalStandardizedLogLineProducerProcessorTest {

    private static final String AUDITLOGG_AS_STRING = "auditlogg as string";

    @Mock
    EnrichedAuditlogg enrichedAuditlogg;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LocalStandardizedLogLineProducerProcessor processor;

    @Test
    void mapToJson_serializesEnrichedAuditloggBody() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody(enrichedAuditlogg);
        when(objectMapper.writeValueAsString(enrichedAuditlogg)).thenReturn(AUDITLOGG_AS_STRING);

        processor.mapToJson(exchange);

        String mappedJson = exchange.getMessage().getBody(String.class);
        assertEquals(AUDITLOGG_AS_STRING, mappedJson);
    }

    //TODO: unit tests for prepareLogLineHeaders
}
