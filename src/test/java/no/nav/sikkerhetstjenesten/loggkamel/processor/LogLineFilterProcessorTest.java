package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
import no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.LogRoutingAttributes;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
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
    AuditLoggArkivDTO auditLoggArkivDTO;

    @Mock
    LogRoutingAttributes logRoutingAttributes;

    @InjectMocks
    LogLineFilterProcessor logLineFilterProcessor;

    @BeforeEach
    void setup() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME)).thenReturn("blah");
        when(exchange.getProperty(AUDIT_LOGG_ARKIV, AuditLoggArkivDTO.class)).thenReturn(auditLoggArkivDTO);
        when(exchange.getProperty(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES, LogRoutingAttributes.class)).thenReturn(logRoutingAttributes);
    }

    @Test
    void isPersonvernAndRead_passesFilter() {
        when(auditLoggArkivDTO.getPersonvern()).thenReturn(true);
        when(logRoutingAttributes.isRead()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

    @Test
    void isArkivAndWrite_passesFilter() {
        when(auditLoggArkivDTO.getArkiv()).thenReturn(true);
        when(logRoutingAttributes.isModification()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

    @Test
    void isOkonomiAndWrite_passesFilter() {
        when(auditLoggArkivDTO.getOkonomi()).thenReturn(true);
        when(logRoutingAttributes.isModification()).thenReturn(true);

        assertTrue(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

    @Test
    void isNotReadOrWrite_removedByFilter() {
        // If no mocked attributes are set, they all default to false
        assertFalse(logLineFilterProcessor.doesLineActionMatchRelevantAuditLoggArkiv(exchange));
    }

}