package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.PLACE_IN_PACKET;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalStandardizedLogLineProducerProcessorTest {

    private static final String AUDITLOGG_AS_STRING = "auditlogg as string";
    private static final String LOG_PACKET_NAME_ROOT = "log_name_root";
    private static final Integer LINE_NUMBER = 1;
    private static final String LOG_PACKET_EXTENSION = ".extension";
    private static final String LOG_PACKET_FILENAME = LOG_PACKET_NAME_ROOT + LOG_PACKET_EXTENSION;
    private static final String LOG_LINE_FILENAME = LOG_PACKET_NAME_ROOT + "." + LINE_NUMBER + LOG_PACKET_EXTENSION;

    @Mock
    EnrichedAuditlogg enrichedAuditlogg;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @InjectMocks
    private LocalStandardizedLogLineProducerProcessor processor;

    @Test
    void mapToJson_serializesEnrichedAuditloggBody() throws Exception {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(EnrichedAuditlogg.class)).thenReturn(enrichedAuditlogg);
        when(objectMapper.writeValueAsString(enrichedAuditlogg)).thenReturn(AUDITLOGG_AS_STRING);

        processor.mapToJson(exchange);

        verify(message).setBody(AUDITLOGG_AS_STRING);
    }

    @Test
    void prepareLogLineHeaders_exceptionIfFilenameMissing() {
        when(exchange.getMessage()).thenReturn(message);

        assertThrows(InvalidLogLineException.class, () -> processor.prepareLogLineHeaders(exchange));

        when(message.getHeader(FILE_NAME, String.class)).thenReturn(LOG_PACKET_FILENAME);

        assertThrows(InvalidLogLineException.class, () -> processor.prepareLogLineHeaders(exchange));
    }

    @Test
    void prepareLogLineHeaders_addsUUIDToFilenameHeader() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(LOG_PACKET_FILENAME);
        when(exchange.getVariable(PLACE_IN_PACKET, Integer.class)).thenReturn(LINE_NUMBER);

        processor.prepareLogLineHeaders(exchange);

        verify(message).setHeader(FILE_NAME, LOG_LINE_FILENAME);
    }
}
