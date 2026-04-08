package no.nav.sikkerhetstjenesten.loggkamel.processor;

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

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogGroupFilterProcessorTest {

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
        String dbName = "blah";
        String extension = "extension";
        String filenameWithExtension = dbName + "." + extension;

        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(filenameWithExtension);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditLoggArkivByDbnameAndTeknologi(dbName, TeknologiEnum.DB2)).thenReturn(null);

        assertFalse(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));
    }

    @Test
    void confirmAuditLoggArkivIsSetAsProperty() {
        String dbName = "blah";
        String extension = "extension";
        String filenameWithExtension = dbName + "." + extension;

        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(filenameWithExtension);

        when(exchange.getProperty(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditLoggArkivByDbnameAndTeknologi(dbName, TeknologiEnum.DB2)).thenReturn(auditLoggArkivResponseDTO);

        assertTrue(logGroupFilterProcessor.isMatchingAuditLoggArkivFound(exchange));

        verify(exchange).setProperty(AUDIT_LOGG_ARKIV, auditLoggArkivResponseDTO);
    }

}