package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogGroupFilterProcessorTest {

    private static final String DBNAME = "dbname";
    private static final String EXTENSION = "extension";
    private static final String FILENAME_WITH_EXTENSION = DBNAME + "." + EXTENSION;

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditLoggArkivResponseDTO auditLoggArkivResponseDTO;

    @Mock
    OversiktService oversiktService;

    @InjectMocks
    LogGroupFilterProcessor logGroupFilterProcessor;

    @Test
    void fileNameNotSet() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(null);

        assertFalse(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));
    }

    @Test
    void fileNameNotSplittable() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn("blah");

        assertFalse(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));
    }

    @Test
    void noMatchingAuditLoggArkiv() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(null);

        assertFalse(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));
    }

    @Test
    void matchingAuditLoggArkivButNotFiksa() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditLoggArkivResponseDTO);
        when(auditLoggArkivResponseDTO.getFiksa()).thenReturn(false);

        assertFalse(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));
    }

    @Test
    void matchingAuditLoggArkivButLoggingNotEnabled() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditLoggArkivResponseDTO);
        when(auditLoggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditLoggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditLoggArkivResponseDTO.getLoggingEndringer()).thenReturn(false);

        assertFalse(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));
    }

    @Test
    void confirmAuditLoggArkivIsSetAsProperty() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditLoggArkivResponseDTO);
        when(auditLoggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditLoggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);

        assertTrue(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));

        verify(exchange).setProperty(AUDIT_LOGG_ARKIV, auditLoggArkivResponseDTO);
    }

}