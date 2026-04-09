package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogRoutingAttributes;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.AUDITLOGG_ARKIV;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogLineFilterProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    LogRoutingAttributes logRoutingAttributes;

    @InjectMocks
    LogLineFilterProcessor logLineFilterProcessor;

    @BeforeEach
    void setup() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME)).thenReturn("blah");
        when(exchange.getProperty(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class)).thenReturn(auditloggArkivResponseDTO);
        when(exchange.getProperty(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES, LogRoutingAttributes.class)).thenReturn(logRoutingAttributes);
    }

    @Test
    void isLoggingLeseoperasjonerAndRead_passesFilter() {
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(logRoutingAttributes.isRead()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

    @Test
    void isLoggingEndringerAndWrite_passesFilter() {
        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(true);
        when(logRoutingAttributes.isModification()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

    @Test
    void isNotReadOrWrite_removedByFilter() {
        // If no mocked attributes are set, they all default to false
        assertFalse(logLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

}