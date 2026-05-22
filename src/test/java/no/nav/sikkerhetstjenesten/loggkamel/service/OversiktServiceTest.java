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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
    void registerLogsReceivedForAuditloggArkiv_successful() {
        assertDoesNotThrow(() -> service.registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI));

        verify(adapter).registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI);
    }

    @Test
    void registerLogsReceivedForAuditloggArkiv_exceptionPassesThrough() {
        doThrow(RuntimeException.class).when(adapter).registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI);

        assertThrows(RuntimeException.class, () -> service.registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI));
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

    @Test
    void naisteamHasActiveArkivTasks_successful() {
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggArkivResponseDTO.getLoggingEndringer()).thenReturn(true);
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditloggArkivResponseDTO));

        assertTrue(service.naisteamHasActiveArkivTasks(NAISTEAM));
    }

    @Test
    void naisteamHasActiveArkivTasks_returnsFalseWhenNoActiveTasks() {
        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(false);
        when(auditloggArkivResponseDTO2.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO2.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggArkivResponseDTO2.getLoggingEndringer()).thenReturn(false);
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditloggArkivResponseDTO, auditloggArkivResponseDTO2));

        assertFalse(service.naisteamHasActiveArkivTasks(NAISTEAM));
    }

    @Test
    void naisteamHasActiveArkivTasks_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.naisteamHasActiveArkivTasks(NAISTEAM));
    }

    @Test
    void findAllNaisteamWithActiveArkivTasks_successful() {
        String activeTeam = "active-team";
        String inactiveTeam = "inactive-team";

        when(auditloggArkivResponseDTO.getFiksa()).thenReturn(true);
        when(auditloggArkivResponseDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(auditloggArkivResponseDTO2.getFiksa()).thenReturn(false);
        when(adapter.findAllDistinctNaisteam()).thenReturn(List.of(activeTeam, inactiveTeam));
        when(adapter.getAllTasksByNaisteam(activeTeam)).thenReturn(List.of(auditloggArkivResponseDTO));
        when(adapter.getAllTasksByNaisteam(inactiveTeam)).thenReturn(List.of(auditloggArkivResponseDTO2));

        assertEquals(List.of(activeTeam), service.findAllNaisteamWithActiveArkivTasks());
    }

    @Test
    void findAllNaisteamWithActiveArkivTasks_exceptionPassesThrough() {
        when(adapter.findAllDistinctNaisteam()).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.findAllNaisteamWithActiveArkivTasks());
    }
}