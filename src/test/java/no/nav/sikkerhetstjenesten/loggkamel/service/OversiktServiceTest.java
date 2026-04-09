package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
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
    AuditloggArkivRequestDTO auditloggArkivRequestDTO;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO2;

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    OversiktService service;

    @Test
    void createAuditloggArkiv_successful() {
        when(adapter.createAuditloggArkiv(auditloggArkivRequestDTO)).thenReturn(auditloggArkivResponseDTO);

        assertEquals(auditloggArkivResponseDTO, service.createAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void createAuditloggArkiv_exceptionPassesThrough() {
        when(adapter.createAuditloggArkiv(auditloggArkivRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.createAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void updateAuditloggArkiv_successful() {
        when(adapter.updateAuditloggArkiv(auditloggArkivRequestDTO)).thenReturn(auditloggArkivResponseDTO);

        assertEquals(auditloggArkivResponseDTO, service.updateAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void updateAuditloggArkiv_exceptionPassesThrough() {
        when(adapter.updateAuditloggArkiv(auditloggArkivRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.updateAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void getAuditloggArkivByDbnameAndTeknologi_successful() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggArkivResponseDTO);

        assertEquals(auditloggArkivResponseDTO, service.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditloggArkivByDbnameAndTeknologi_exceptionPassesThrough() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditloggArkivByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditloggArkivByNaisteam_successful() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditloggArkivResponseDTO, auditloggArkivResponseDTO2));

        assertEquals(List.of(auditloggArkivResponseDTO, auditloggArkivResponseDTO2), service.getAuditloggArkivByNaisteam(NAISTEAM));
    }

    @Test
    void getAuditloggArkivByNaisteam_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditloggArkivByNaisteam(NAISTEAM));
    }
}