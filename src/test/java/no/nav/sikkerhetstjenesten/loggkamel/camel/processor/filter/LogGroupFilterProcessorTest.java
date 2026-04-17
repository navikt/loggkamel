package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogGroupFilterProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @InjectMocks
    LogGroupFilterProcessor logGroupFilterProcessor;

    @BeforeEach
    void setup() {
        when(exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class)).thenReturn(auditloggArkivResponseDTO);
    }

    @Test
    void matchingAuditloggArkivButNotFiksa() {
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(false);

        when(auditloggArkivResponseDTO.getTeknologi()).thenReturn(TeknologiEnum.DB2);

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void matchingAuditloggArkivButLoggingNotEnabled() {
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(false);

        when(auditloggArkivResponseDTO.getTeknologi()).thenReturn(TeknologiEnum.DB2);

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void logGroupPassesFilter() {
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);

        assertTrue(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

}