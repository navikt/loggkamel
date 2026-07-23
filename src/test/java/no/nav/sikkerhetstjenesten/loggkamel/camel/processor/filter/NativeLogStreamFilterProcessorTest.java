package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_TASK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeLogStreamFilterProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO;

    @InjectMocks
    NativeLogStreamFilterProcessor nativeLogStreamFilterProcessor;

    @BeforeEach
    void setup() {
        when(exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class)).thenReturn(auditloggTaskDTO);
    }

    @Test
    void matchingAuditloggTaskButNotFiksa() {
        when(auditloggTaskDTO.getFiksa()).thenReturn(false);

        assertThrows(InvalidLogStreamException.class, () -> nativeLogStreamFilterProcessor.doesAuditloggTaskRequireForwardingLogs(exchange));
    }

    @Test
    void matchingAuditloggTaskButLoggingNotEnabled() {
        when(auditloggTaskDTO.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggTaskDTO.getLoggingEndringer()).thenReturn(false);

        when(auditloggTaskDTO.getTeknologi()).thenReturn(TeknologiEnum.DB2);

        assertFalse(nativeLogStreamFilterProcessor.doesAuditloggTaskRequireForwardingLogs(exchange));
    }

    @Test
    void logGroupPassesFilter() {
        when(auditloggTaskDTO.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(true);

        assertTrue(nativeLogStreamFilterProcessor.doesAuditloggTaskRequireForwardingLogs(exchange));
    }

}