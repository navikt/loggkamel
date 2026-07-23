package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
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
    AuditloggTaskRequestDTO auditloggTaskRequestDTO;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO2;

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    OversiktService service;

    @Test
    void createAuditloggTask_successful() {
        when(adapter.createAuditloggTask(auditloggTaskRequestDTO)).thenReturn(auditloggTaskDTO);

        assertEquals(auditloggTaskDTO, service.createAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void createAuditloggTask_exceptionPassesThrough() {
        when(adapter.createAuditloggTask(auditloggTaskRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.createAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_successful() {
        when(adapter.updateAuditloggTask(auditloggTaskRequestDTO)).thenReturn(auditloggTaskDTO);

        assertEquals(auditloggTaskDTO, service.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_exceptionPassesThrough() {
        when(adapter.updateAuditloggTask(auditloggTaskRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void getAuditloggTaskByDbnameAndTeknologi_successful() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggTaskDTO);

        assertEquals(auditloggTaskDTO, service.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditloggTaskByDbnameAndTeknologi_exceptionPassesThrough() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggTask_successful() {
        assertDoesNotThrow(() -> service.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));

        verify(adapter).registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI);
    }

    @Test
    void registerLogsReceivedForAuditloggTask_exceptionPassesThrough() {
        doThrow(RuntimeException.class).when(adapter).registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI);

        assertThrows(RuntimeException.class, () -> service.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAuditloggTaskByNaisteam_successful() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditloggTaskDTO, auditloggTaskDTO2));

        assertEquals(List.of(auditloggTaskDTO, auditloggTaskDTO2), service.getAuditloggTaskByNaisteam(NAISTEAM));
    }

    @Test
    void getAuditloggTaskByNaisteam_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditloggTaskByNaisteam(NAISTEAM));
    }

    @Test
    void naisteamHasActiveAuditloggTasks_successful() {
        when(auditloggTaskDTO.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggTaskDTO.getLoggingEndringer()).thenReturn(true);
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditloggTaskDTO));

        assertTrue(service.naisteamHasActiveAuditloggTasks(NAISTEAM));
    }

    @Test
    void naisteamHasActiveAuditloggTasks_returnsFalseWhenNoActiveTasks() {
        when(auditloggTaskDTO.getFiksa()).thenReturn(false);
        when(auditloggTaskDTO2.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO2.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggTaskDTO2.getLoggingEndringer()).thenReturn(false);
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(auditloggTaskDTO, auditloggTaskDTO2));

        assertFalse(service.naisteamHasActiveAuditloggTasks(NAISTEAM));
    }

    @Test
    void naisteamHasActiveAuditloggTasks_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.naisteamHasActiveAuditloggTasks(NAISTEAM));
    }

    @Test
    void findAllNaisteamWithActiveAuditloggTasks_successful() {
        String activeTeam = "active-team";
        String inactiveTeam = "inactive-team";

        when(auditloggTaskDTO.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO.getLoggingLeseoperasjoner()).thenReturn(true);
        when(auditloggTaskDTO2.getFiksa()).thenReturn(false);
        when(adapter.findAllDistinctNaisteam()).thenReturn(List.of(activeTeam, inactiveTeam));
        when(adapter.getAllTasksByNaisteam(activeTeam)).thenReturn(List.of(auditloggTaskDTO));
        when(adapter.getAllTasksByNaisteam(inactiveTeam)).thenReturn(List.of(auditloggTaskDTO2));

        assertEquals(List.of(activeTeam), service.findAllNaisteamWithActiveAuditloggTasks());
    }

    @Test
    void findAllNaisteamWithActiveAuditloggTasks_exceptionPassesThrough() {
        when(adapter.findAllDistinctNaisteam()).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.findAllNaisteamWithActiveAuditloggTasks());
    }
}