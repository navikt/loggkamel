package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OversiktServiceTest {

    private static final String DBNAME = "dbName";
    private static final TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private static final String NAISTEAM = "naisteam";

    @Mock
    AuditLoggArkivDTO requestAuditLoggArkivDTO;

    @Mock
    AuditLoggArkivDTO responseAuditLoggArkivDTO;

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    OversiktService service;

    @Test
    void createAuditLoggArkiv_successful() {
        when(adapter.createAuditLoggArkiv(requestAuditLoggArkivDTO)).thenReturn(responseAuditLoggArkivDTO);

        assertEquals(responseAuditLoggArkivDTO, service.createAuditLoggArkiv(requestAuditLoggArkivDTO));
    }

    @Test
    void createAuditLoggArkiv_exceptionPassesThrough() {
        when(adapter.createAuditLoggArkiv(requestAuditLoggArkivDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.createAuditLoggArkiv(requestAuditLoggArkivDTO));
    }

    @Test
    void updateAuditLoggArkiv_successful() {
        when(adapter.updateAuditLoggArkiv(requestAuditLoggArkivDTO)).thenReturn(responseAuditLoggArkivDTO);

        assertEquals(responseAuditLoggArkivDTO, service.updateAuditLoggArkiv(requestAuditLoggArkivDTO));
    }

    @Test
    void updateAuditLoggArkiv_exceptionPassesThrough() {
        when(adapter.updateAuditLoggArkiv(requestAuditLoggArkivDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.updateAuditLoggArkiv(requestAuditLoggArkivDTO));
    }

    @Test
    void getAuditLoggArkivByDbnameAndTeknologi_successful() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(responseAuditLoggArkivDTO);

        assertEquals(responseAuditLoggArkivDTO, service.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditLoggArkivByDbnameAndTeknologi_exceptionPassesThrough() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditLoggArkivByNaisteam_successful() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(requestAuditLoggArkivDTO, responseAuditLoggArkivDTO));

        assertEquals(List.of(requestAuditLoggArkivDTO, responseAuditLoggArkivDTO), service.getAuditLoggArkivByNaisteam(NAISTEAM));
    }

    @Test
    void getAuditLoggArkivByNaisteam_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditLoggArkivByNaisteam(NAISTEAM));
    }
}