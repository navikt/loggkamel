package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DatabaseDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.AUDITLOGG_ARKIV;
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
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    OversiktService oversiktService;

    @InjectMocks
    LogGroupFilterProcessor logGroupFilterProcessor;

    @Test
    void fileNameNotSet() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(null);

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void fileNameNotSplittable() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn("blah");

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void errorWhenFetchingAuditloggArkiv() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenThrow(new RuntimeException("Database error"));

        assertThrows(DatabaseDependencyException.class, () -> logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void noMatchingAuditloggArkiv() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(null);

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void matchingAuditloggArkivButNotFiksa() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggArkivResponseDTO);
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(false);

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void matchingAuditloggArkivButLoggingNotEnabled() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggArkivResponseDTO);
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(false);

        assertFalse(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));
    }

    @Test
    void confirmAuditloggArkivIsSetAsProperty() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggArkivResponseDTO);
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);

        assertTrue(logGroupFilterProcessor.isMatchingAuditloggArkivFound(exchange));

        verify(exchange).setProperty(AUDITLOGG_ARKIV, auditloggArkivResponseDTO);
    }

}