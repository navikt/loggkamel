package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogLineOperationTypes;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
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
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    LogLineOperationTypes logLineOperationTypes;

    @InjectMocks
    StandardizedLogLineFilterProcessor standardizedLogLineFilterProcessor;

    void setupDoesLineActionMatchRelevantAuditloggArkiv() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME)).thenReturn("blah");
        when(exchange.getVariable(PLACE_IN_PACKET)).thenReturn(1);
        when(exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class)).thenReturn(auditloggArkivResponseDTO);
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
    void doesLineActionMatchRelevantAuditloggArkiv_passesIfBothIsReadingAndForwardingReads() {
        setupDoesLineActionMatchRelevantAuditloggArkiv();

        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(logLineOperationTypes.isRead()).thenReturn(true);

        assertTrue(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggArkiv_passesIfBothIsWriteAndForwardingWrites() {
        setupDoesLineActionMatchRelevantAuditloggArkiv();

        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(true);
        when(logLineOperationTypes.isModification()).thenReturn(true);

        assertTrue(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggArkiv_removedIfIsNeitherReadNorWrite() {
        setupDoesLineActionMatchRelevantAuditloggArkiv();

        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(true);

        when(logLineOperationTypes.isRead()).thenReturn(false);
        when(logLineOperationTypes.isModification()).thenReturn(false);

        assertFalse(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

    @Test
    void doesLineActionMatchRelevantAuditloggArkiv_removedIfNoOperationsAreForwarded() {
        setupDoesLineActionMatchRelevantAuditloggArkiv();

        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(false);

        // If no mocked attributes are set, they all default to false
        assertFalse(standardizedLogLineFilterProcessor.doesLineActionMatchRelevantAuditloggArkiv(exchange));
    }

}