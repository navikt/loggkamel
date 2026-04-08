package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
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
    AuditLoggArkivRequestDTO auditLoggArkivRequestDTO;

    @Mock
    AuditLoggArkivResponseDTO auditLoggArkivResponseDTO;

    @Mock
    AuditLoggArkivResponseDTO auditLoggArkivResponseDTO2;

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    OversiktService service;

    @Test
    void createAuditLoggArkiv_successful() {
        when(adapter.createAuditLoggArkiv(auditLoggArkivRequestDTO)).thenReturn(auditLoggArkivResponseDTO);

        assertEquals(auditLoggArkivResponseDTO, service.createAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void createAuditLoggArkiv_exceptionPassesThrough() {
        when(adapter.createAuditLoggArkiv(auditLoggArkivRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.createAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void updateAuditLoggArkiv_successful() {
        when(adapter.updateAuditLoggArkiv(auditLoggArkivRequestDTO)).thenReturn(auditLoggArkivResponseDTO);

        assertEquals(auditLoggArkivResponseDTO, service.updateAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void updateAuditLoggArkiv_exceptionPassesThrough() {
        when(adapter.updateAuditLoggArkiv(auditLoggArkivRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.updateAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void getAuditLoggArkivByDbnameAndTeknologi_successful() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditLoggArkivResponseDTO);

        assertEquals(auditLoggArkivResponseDTO, service.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditLoggArkivByDbnameAndTeknologi_exceptionPassesThrough() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditLoggArkivByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditLoggArkivByNaisteam_successful() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditLoggArkivResponseDTO, auditLoggArkivResponseDTO2));

        assertEquals(List.of(auditLoggArkivResponseDTO, auditLoggArkivResponseDTO2), service.getAuditLoggArkivByNaisteam(NAISTEAM));
    }

    @Test
    void getAuditLoggArkivByNaisteam_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditLoggArkivByNaisteam(NAISTEAM));
    }
}