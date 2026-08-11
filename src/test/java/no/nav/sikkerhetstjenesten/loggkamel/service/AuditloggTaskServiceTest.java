package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisTeamDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditloggTaskServiceTest {

    private static final String DBNAME = "dbName";
    private static final TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private static final String NAISTEAM_1 = "naisteam1";
    private static final String NAISTEAM_2 = "naisteam2";

    @Mock
    AuditloggTaskRequestDTO auditloggTaskRequestDTO;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO1;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO2;

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    AuditloggTaskService service;

    @Test
    void createAuditloggTask_successful() {
        when(adapter.createAuditloggTask(auditloggTaskRequestDTO)).thenReturn(auditloggTaskDTO1);

        assertEquals(auditloggTaskDTO1, service.createAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void createAuditloggTask_exceptionPassesThrough() {
        when(adapter.createAuditloggTask(auditloggTaskRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.createAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_successful() {
        when(adapter.updateAuditloggTask(auditloggTaskRequestDTO)).thenReturn(auditloggTaskDTO1);

        assertEquals(auditloggTaskDTO1, service.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_exceptionPassesThrough() {
        when(adapter.updateAuditloggTask(auditloggTaskRequestDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void getAuditloggTaskByDbnameAndTeknologi_successful() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggTaskDTO1);

        assertEquals(auditloggTaskDTO1, service.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
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
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_1)).thenReturn(List.of(auditloggTaskDTO1, auditloggTaskDTO2));

        assertEquals(List.of(auditloggTaskDTO1, auditloggTaskDTO2), service.getAuditloggTaskByNaisteam(NAISTEAM_1));
    }

    @Test
    void getAuditloggTaskByNaisteam_exceptionPassesThrough() {
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_1)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAuditloggTaskByNaisteam(NAISTEAM_1));
    }

    @Test
    void getAllTasksGroupedByNaisteam_noRegisteredTasks() {
        when(adapter.findAllDistinctNaisteam()).thenReturn(Collections.emptyList());

        List<NaisTeamDTO> tasksGroupedByTeam =  service.getAllTasksGroupedByNaisteam();

        assertEquals(Collections.emptyList(), tasksGroupedByTeam);
    }

    @Test
    void getAllTasksGroupedByNaisteam_success() {
        when(adapter.findAllDistinctNaisteam()).thenReturn(List.of(NAISTEAM_1, NAISTEAM_2));
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_1)).thenReturn(List.of(auditloggTaskDTO1));
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_2)).thenReturn(List.of(auditloggTaskDTO2));

        List<NaisTeamDTO> tasksGroupedByTeam =  service.getAllTasksGroupedByNaisteam();

        NaisTeamDTO naisTeamDTOForNaisTeam1 = NaisTeamDTO.builder()
                .naisteam(NAISTEAM_1)
                .tasksForTeam(List.of(auditloggTaskDTO1))
                .build();

        NaisTeamDTO naisTeamDTOForNaisTeam2 = NaisTeamDTO.builder()
                .naisteam(NAISTEAM_2)
                .tasksForTeam(List.of(auditloggTaskDTO2))
                .build();

        assertTrue(tasksGroupedByTeam.containsAll(List.of(naisTeamDTOForNaisTeam1,  naisTeamDTOForNaisTeam2)));
    }

    @Test
    void getAllTasksGroupedByNaisteam_exceptionPassesThrough() {
        when(adapter.findAllDistinctNaisteam()).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getAllTasksGroupedByNaisteam());
    }

    @Test
    void naisteamHasActiveAuditloggTasks_successful() {
        when(auditloggTaskDTO1.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO1.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggTaskDTO1.getLoggingEndringer()).thenReturn(true);
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_1)).thenReturn(List.of(auditloggTaskDTO1));

        assertTrue(service.naisteamHasActiveAuditloggTasks(NAISTEAM_1));
    }

    @Test
    void naisteamHasActiveAuditloggTasks_returnsFalseWhenNoActiveTasks() {
        when(auditloggTaskDTO1.getFiksa()).thenReturn(false);
        when(auditloggTaskDTO2.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO2.getLoggingLeseoperasjoner()).thenReturn(false);
        when(auditloggTaskDTO2.getLoggingEndringer()).thenReturn(false);
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_1)).thenReturn(List.of(auditloggTaskDTO1, auditloggTaskDTO2));

        assertFalse(service.naisteamHasActiveAuditloggTasks(NAISTEAM_1));
    }

    @Test
    void naisteamHasActiveAuditloggTasks_exceptionPassesThrough() {
        when(adapter.getTasksRegisteredToNaisteam(NAISTEAM_1)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.naisteamHasActiveAuditloggTasks(NAISTEAM_1));
    }

    @Test
    void findAllNaisteamWithActiveAuditloggTasks_successful() {
        String activeTeam = "active-team";
        String inactiveTeam = "inactive-team";

        when(auditloggTaskDTO1.getFiksa()).thenReturn(true);
        when(auditloggTaskDTO1.getLoggingLeseoperasjoner()).thenReturn(true);
        when(auditloggTaskDTO2.getFiksa()).thenReturn(false);
        when(adapter.findAllDistinctNaisteam()).thenReturn(List.of(activeTeam, inactiveTeam));
        when(adapter.getTasksRegisteredToNaisteam(activeTeam)).thenReturn(List.of(auditloggTaskDTO1));
        when(adapter.getTasksRegisteredToNaisteam(inactiveTeam)).thenReturn(List.of(auditloggTaskDTO2));

        assertEquals(List.of(activeTeam), service.findAllNaisteamWithActiveAuditloggTasks());
    }

    @Test
    void findAllNaisteamWithActiveAuditloggTasks_exceptionPassesThrough() {
        when(adapter.findAllDistinctNaisteam()).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.findAllNaisteamWithActiveAuditloggTasks());
    }
}