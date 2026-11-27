package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.PostgresInvalidMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledPostgresBackfillTest {

    @Mock
    AuditloggTaskService auditloggTaskService;

    @Mock
    PostgresInvalidMessageService invalidMessageService;

    @Mock
    AdvisoryLockService advisoryLockService;

    @Mock
    Metrics metrics;

    @InjectMocks
    ScheduledPostgresBackfill scheduledBackfill;

    @Test
    void scheduledBackfillSkipsProcessingWhenPostgresLockIsHeld() {
        when(advisoryLockService.runIfLockAcquired(anyLong(), any(Runnable.class))).thenReturn(false);

        scheduledBackfill.backfillPostgresLogsForAllRequestedTasks();

        verifyNoInteractions(auditloggTaskService, invalidMessageService);
    }

    @Test
    void backfillLogsForAllRequestedTasksPropagatesTaskSelectionFailure() {
        RuntimeException failure = new IllegalStateException("selection failed");
        when(auditloggTaskService.findConfiguredTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED)).thenThrow(failure);

        assertSame(failure, assertThrows(
                RuntimeException.class, scheduledBackfill::backfillLogsForAllRequestedTasks));
    }

    @Test
    void backfillLogsForAllRequestedTasksContinuesAfterOneTaskFails() {
        AuditloggTaskDTO failedTask = task("failed-db");
        AuditloggTaskDTO nextTask = task("next-db");
        when(auditloggTaskService.findConfiguredTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED))
                .thenReturn(List.of(failedTask, nextTask));
        doThrow(new IllegalStateException("transfer failed"))
                .when(invalidMessageService).moveLogsToConsumerBucket(failedTask);

        scheduledBackfill.backfillLogsForAllRequestedTasks();

        verify(invalidMessageService).moveLogsToConsumerBucket(failedTask);
        verify(invalidMessageService).moveLogsToConsumerBucket(nextTask);
    }

    @Test
    void backfillLogsForAllRequestedTasksProcessesEverySelectedTask() {
        AuditloggTaskDTO firstTask = task("first-db");
        AuditloggTaskDTO secondTask = task("second-db");
        when(auditloggTaskService.findConfiguredTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED))
                .thenReturn(List.of(firstTask, secondTask));

        scheduledBackfill.backfillLogsForAllRequestedTasks();

        verify(invalidMessageService).moveLogsToConsumerBucket(firstTask);
        verify(invalidMessageService).moveLogsToConsumerBucket(secondTask);
    }

    @Test
    void backfillSingleTaskLeavesRequestedStatusWhenTransferFails() {
        AuditloggTaskDTO task = task("failed-db");
        when(auditloggTaskService.findConfiguredTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED)).thenReturn(List.of(task));
        doThrow(new IllegalStateException("transfer failed"))
                .when(invalidMessageService).moveLogsToConsumerBucket(task);

        scheduledBackfill.backfillLogsForAllRequestedTasks();

        verify(auditloggTaskService, never()).setBackfillStatus(
                "failed-db", TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_COMPLETED);
    }

    @Test
    void backfillSingleTaskCompletesAfterAllFilesHaveBeenMoved() {
        AuditloggTaskDTO task = task("moved-files-db");
        when(auditloggTaskService.findConfiguredTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED)).thenReturn(List.of(task));

        scheduledBackfill.backfillLogsForAllRequestedTasks();

        InOrder order = inOrder(invalidMessageService, auditloggTaskService);
        order.verify(invalidMessageService).moveLogsToConsumerBucket(task);
        order.verify(auditloggTaskService).setBackfillStatus(
                "moved-files-db", TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_COMPLETED);
    }

    private static AuditloggTaskDTO task(String dbname) {
        AuditloggTaskDTO task = mock(AuditloggTaskDTO.class);
        when(task.getDbname()).thenReturn(dbname);
        return task;
    }
}
