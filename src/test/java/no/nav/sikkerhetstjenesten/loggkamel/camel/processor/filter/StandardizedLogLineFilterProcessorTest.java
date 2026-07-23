package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogLineOperationTypes;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_TASK;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.PLACE_IN_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.MESSAGE_SHOULD_BE_SKIPPED;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandardizedLogLineFilterProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO;

    @Mock
    LogLineOperationTypes logLineOperationTypes;

    @InjectMocks
    StandardizedLogLineFilterProcessor standardizedLogLineFilterProcessor;

    void setupDoesLineActionMatchRelevantAuditloggTask() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME)).thenReturn("blah");
        when(exchange.getVariable(PLACE_IN_PACKET)).thenReturn(1);
        when(exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class)).thenReturn(auditloggTaskDTO);
        when(exchange.getVariable(LogLineOperationTypes.LOG_LINE_OPERATION_TYPES, LogLineOperationTypes.class)).thenReturn(logLineOperationTypes);
    }

    @Test
    void messageIsMissingImmediateSkipHeader_failsFilterOnSkipHeaderPresence() {
        when(exchange.getVariable(MESSAGE_SHOULD_BE_SKIPPED, Boolean.class)).thenReturn(Boolean.TRUE);

        assertFalse(standardizedLogLineFilterProcessor.messageIsMissingImmediateSkipHeader(exchange));
    }

    @Test
    void messageIsMissingImmediateSkipHeader_passesFilterOnSkipHeaderAbsence() {
        assertTrue(standardizedLogLineFilterProcessor.messageIsMissingImmediateSkipHeader(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggTask_passesIfBothIsReadingAndForwardingReads() {
        setupDoesLineActionMatchRelevantAuditloggTask();

        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(logLineOperationTypes.isRead()).thenReturn(true);

        assertTrue(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggTask(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggTask_passesIfBothIsWriteAndForwardingWrites() {
        setupDoesLineActionMatchRelevantAuditloggTask();

        when(auditloggTaskDTO.getLoggingEndringer()).thenReturn(true);
        when(logLineOperationTypes.isModification()).thenReturn(true);

        assertTrue(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggTask(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggTask_removedIfIsNeitherReadNorWrite() {
        setupDoesLineActionMatchRelevantAuditloggTask();

        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(auditloggTaskDTO.getLoggingEndringer()).thenReturn(true);

        when(logLineOperationTypes.isRead()).thenReturn(false);
        when(logLineOperationTypes.isModification()).thenReturn(false);

        assertFalse(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggTask(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggTask_removedIfNoOperationsAreForwarded() {
        setupDoesLineActionMatchRelevantAuditloggTask();

        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggTaskDTO.getLoggingEndringer()).thenReturn(false);

        // If no mocked attributes are set, they all default to false
        assertFalse(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggTask(exchange));
    }

}