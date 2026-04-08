package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogRoutingAttributes;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
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
    AuditLoggArkivResponseDTO auditLoggArkivResponseDTO;

    @Mock
    LogRoutingAttributes logRoutingAttributes;

    @InjectMocks
    LogLineFilterProcessor logLineFilterProcessor;

    @BeforeEach
    void setup() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME)).thenReturn("blah");
        when(exchange.getProperty(AUDIT_LOGG_ARKIV, AuditLoggArkivResponseDTO.class)).thenReturn(auditLoggArkivResponseDTO);
        when(exchange.getProperty(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES, LogRoutingAttributes.class)).thenReturn(logRoutingAttributes);
    }

    @Test
    void isLeseoperasjonerAndRead_passesFilter() {
        when(auditLoggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(logRoutingAttributes.isRead()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

    @Test
    void isArkivAndWrite_passesFilter() {
        when(auditLoggArkivResponseDTO.getArkiv()).thenReturn(true);
        when(logRoutingAttributes.isModification()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

    @Test
    void isOkonomiAndWrite_passesFilter() {
        when(auditLoggArkivResponseDTO.getOkonomi()).thenReturn(true);
        when(logRoutingAttributes.isModification()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

    @Test
    void isNotReadOrWrite_removedByFilter() {
        // If no mocked attributes are set, they all default to false
        assertFalse(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

}