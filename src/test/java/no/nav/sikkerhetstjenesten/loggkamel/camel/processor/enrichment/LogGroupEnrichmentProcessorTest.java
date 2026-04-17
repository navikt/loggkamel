package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DatabaseDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.NaisService;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogGroupEnrichmentProcessorTest {

    private static final String DBNAME = "dbname";
    private static final String EXTENSION = "extension";
    private static final String FILENAME_WITH_EXTENSION = DBNAME + "." + EXTENSION;
    private static final String NAIS_TEAM = "naisteam";
    private static final String GCP_PROJECT_ID = "gcpProjectId";

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    OversiktService oversiktService;

    @Mock
    NaisService naisService;

    @InjectMocks
    LogGroupEnrichmentProcessor logGroupEnrichmentProcessor;

    @Test
    void fileNameNotSet() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(null);

        assertThrows(InvalidPostgresLogGroupException.class, () -> logGroupEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void fileNameNotSplittable() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn("blah");

        assertThrows(InvalidPostgresLogGroupException.class, () -> logGroupEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void errorWhenFetchingAuditloggArkiv() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenThrow(new RuntimeException("Database error"));

        assertThrows(DatabaseDependencyException.class, () -> logGroupEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void noMatchingAuditloggArkiv() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(null);

        assertThrows(InvalidPostgresLogGroupException.class, () -> logGroupEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void exceptionCallingNaisService_exceptionPassesThrough() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggArkivResponseDTO);

        when(auditloggArkivResponseDTO.getNaisteam()).thenReturn(NAIS_TEAM);

        when(naisService.getCurrentEnvGCPIDForTeam(NAIS_TEAM)).thenThrow(new RuntimeException("Nais service error"));

        assertThrows(RuntimeException.class, () -> logGroupEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void happyPath() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(FILENAME_WITH_EXTENSION);

        when(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TeknologiEnum.DB2);

        when(oversiktService.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggArkivResponseDTO);

        when(auditloggArkivResponseDTO.getNaisteam()).thenReturn(NAIS_TEAM);

        when(naisService.getCurrentEnvGCPIDForTeam(NAIS_TEAM)).thenReturn(GCP_PROJECT_ID);

        assertDoesNotThrow(() -> logGroupEnrichmentProcessor.enrich(exchange));
    }

}