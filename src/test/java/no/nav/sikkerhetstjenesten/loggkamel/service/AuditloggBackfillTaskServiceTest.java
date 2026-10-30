package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditloggBackfillTaskServiceTest {

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    AuditloggTaskService service;

    @Test
    void findRequestedBackfillTasksGroupsBothRequestedStatesAcrossTechnologies() {
        AuditloggTaskDTO backfillTask = task("team-a", "db-a", TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED);
        AuditloggTaskDTO coldStorageTask = task("team-a", "db-b", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_REQUESTED);
        AuditloggTaskDTO taskWithoutTeam = task(null, "db-c", TeknologiEnum.POSTGRESQL, BackfillStatus.COLD_STORAGE_REQUESTED);
        when(adapter.findTasksByBackfillStatuses(List.of(
                BackfillStatus.BACKFILL_REQUESTED, BackfillStatus.COLD_STORAGE_REQUESTED)))
                .thenReturn(List.of(backfillTask, coldStorageTask, taskWithoutTeam));

        List<NaisteamDTO> result = service.findRequestedBackfillTasksGroupedByNaisteam();

        assertEquals(2, result.size());
        assertEquals("team-a", result.getFirst().getNaisteam());
        assertEquals(List.of(backfillTask, coldStorageTask), result.getFirst().getTasksForTeam());
        assertEquals("unassigned", result.getLast().getNaisteam());
        assertEquals(List.of(taskWithoutTeam), result.getLast().getTasksForTeam());
    }

    @Test
    void completeRequestedTaskMovesDB2ColdStorageRequestToCompleted() {
        AuditloggTaskDTO task = task("team-a", "db-a", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_REQUESTED);
        when(adapter.findByDbnameAndTeknologi("db-a", TeknologiEnum.DB2)).thenReturn(task);

        service.completeRequestedTask("db-a", TeknologiEnum.DB2);

        verify(adapter).setBackfillStatus("db-a", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
    }

    @Test
    void completeRequestedTaskMovesPostgresRequestToMatchingCompletedState() {
        for (BackfillStatus requestedStatus : List.of(
                BackfillStatus.BACKFILL_REQUESTED, BackfillStatus.COLD_STORAGE_REQUESTED)) {
            AuditloggTaskDTO task = task("team-a", "db-a", TeknologiEnum.POSTGRESQL, requestedStatus);
            when(adapter.findByDbnameAndTeknologi("db-a", TeknologiEnum.POSTGRESQL)).thenReturn(task);

            service.completeRequestedTask("db-a", TeknologiEnum.POSTGRESQL);

            BackfillStatus completedStatus = requestedStatus == BackfillStatus.BACKFILL_REQUESTED
                    ? BackfillStatus.BACKFILL_COMPLETED
                    : BackfillStatus.COLD_STORAGE_COMPLETED;
            verify(adapter).setBackfillStatus("db-a", TeknologiEnum.POSTGRESQL, completedStatus);
            clearInvocations(adapter);
        }
    }

    @Test
    void completeRequestedTaskRejectsDB2BackfillRequest() {
        when(adapter.findByDbnameAndTeknologi("db-a", TeknologiEnum.DB2))
                .thenReturn(task("team-a", "db-a", TeknologiEnum.DB2, BackfillStatus.BACKFILL_REQUESTED));

        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.completeRequestedTask("db-a", TeknologiEnum.DB2));

        verify(adapter, never()).setBackfillStatus(anyString(), any(), any());
    }

    @Test
    void completeRequestedTaskReturnsNotFoundForMissingTask() {
        when(adapter.findByDbnameAndTeknologi("db-a", TeknologiEnum.DB2)).thenReturn(null);

        assertThrows(AuditloggTaskNotFoundException.class,
                () -> service.completeRequestedTask("db-a", TeknologiEnum.DB2));

        verify(adapter, never()).setBackfillStatus(anyString(), any(), any());
    }

    private static AuditloggTaskDTO task(
            String naisteam, String dbName, TeknologiEnum technology, BackfillStatus status) {
        return AuditloggTaskDTO.builder()
                .naisteam(naisteam)
                .dbname(dbName)
                .teknologi(technology)
                .backfill(status)
                .okonomi(false)
                .arkivforskrift(false)
                .endringerUtenKrav(false)
                .loggingLeseoperasjoner(false)
                .fiksa(true)
                .loggingEndringer(false)
                .funnetLogger(false)
                .discardLogs(false)
                .build();
    }
}
