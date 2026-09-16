package no.nav.sikkerhetstjenesten.loggkamel.service;

import io.getunleash.Unleash;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.DailyDB2LogPuller.DB2_PULL_FEATURE_FLAG;
import static no.nav.sikkerhetstjenesten.loggkamel.service.DailyDB2LogPuller.DB2_PULL_LOCK_KEY;
import static no.nav.sikkerhetstjenesten.loggkamel.service.DailyDB2LogPuller.PULL_END_DAYS_AGO;
import static no.nav.sikkerhetstjenesten.loggkamel.service.DailyDB2LogPuller.PULL_START_DAYS_AGO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyDB2LogPullerTest {

    private static final String DBNAME_1 = "dbOne";
    private static final String DBNAME_2 = "dbTwo";

    private final LocalDate expectedStartDate = LocalDate.now().minusDays(PULL_START_DAYS_AGO);
    private final LocalDate expectedEndDate = LocalDate.now().minusDays(PULL_END_DAYS_AGO);

    @Mock
    AuditloggTaskService auditloggTaskService;

    @Mock
    DB2PacketService db2PacketService;

    @Mock
    PullRerunService pullRerunService;

    @Mock
    AdvisoryLockService advisoryLockService;

    @Mock
    Metrics metrics;

    @Mock
    Unleash unleash;

    @Mock
    AuditloggTaskDTO task1;

    @Mock
    AuditloggTaskDTO task2;

    @Mock
    AuditloggTaskDTO task3;

    @InjectMocks
    DailyDB2LogPuller task;

    private void enableFeatureFlag() {
        when(unleash.isEnabled(DB2_PULL_FEATURE_FLAG, false)).thenReturn(true);
    }

    private void grantLock() {
        when(advisoryLockService.runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(1, Runnable.class).run();
                    return true;
                });
    }

    @Test
    void pullDB2LogsForAllActiveTasks_doesNothingWhenFeatureFlagIsDisabled() {
        when(unleash.isEnabled(DB2_PULL_FEATURE_FLAG, false)).thenReturn(false);

        task.pullDB2LogsForAllActiveTasks();

        verifyNoInteractions(advisoryLockService);
        verifyNoInteractions(auditloggTaskService);
        verifyNoInteractions(db2PacketService);
    }

    @Test
    void pullDB2LogsForAllActiveTasks_doesNotPullWhenLockIsHeldByAnotherInstance() {
        enableFeatureFlag();
        when(advisoryLockService.runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class))).thenReturn(false);

        task.pullDB2LogsForAllActiveTasks();

        verifyNoInteractions(auditloggTaskService);
        verifyNoInteractions(db2PacketService);
        verifyNoInteractions(pullRerunService);
    }

    @Test
    void pullDB2LogsForAllActiveTasks_pullsTwoPrecedingDaysForEveryActiveTask() {
        enableFeatureFlag();
        grantLock();
        when(auditloggTaskService.findActiveTasksByTeknologi(TeknologiEnum.DB2)).thenReturn(List.of(task1, task2));

        task.pullDB2LogsForAllActiveTasks();

        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(task1, expectedStartDate, expectedEndDate);
        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(task2, expectedStartDate, expectedEndDate);
        verifyNoInteractions(pullRerunService);
        verifyNoInteractions(metrics);
    }

    @Test
    void pullDB2LogsForAllActiveTasks_continuesWithRemainingTasksAndRegistersRerunWhenOneTaskFails() {
        enableFeatureFlag();
        grantLock();
        when(task2.getDbname()).thenReturn(DBNAME_2);
        when(auditloggTaskService.findActiveTasksByTeknologi(TeknologiEnum.DB2)).thenReturn(List.of(task1, task2, task3));

        RuntimeException pullFailure = new RuntimeException("proxy unavailable");
        doThrow(pullFailure).when(db2PacketService)
                .fetchLogsWithinDateRangeAndPersistAsPackets(task2, expectedStartDate, expectedEndDate);

        task.pullDB2LogsForAllActiveTasks();

        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(task1, expectedStartDate, expectedEndDate);
        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(task3, expectedStartDate, expectedEndDate);
        verify(pullRerunService, times(1))
                .registerFailedPull(DBNAME_2, TeknologiEnum.DB2, expectedStartDate, expectedEndDate, pullFailure);
        verify(metrics).incrementPullFailure(DBNAME_2, TeknologiEnum.DB2);
    }

    @Test
    void pullDB2LogsForAllActiveTasks_continuesWhenRegisteringTheFailureItselfFails() {
        enableFeatureFlag();
        grantLock();
        when(task1.getDbname()).thenReturn(DBNAME_1);
        when(auditloggTaskService.findActiveTasksByTeknologi(TeknologiEnum.DB2)).thenReturn(List.of(task1, task2));

        doThrow(new RuntimeException("proxy unavailable")).when(db2PacketService)
                .fetchLogsWithinDateRangeAndPersistAsPackets(task1, expectedStartDate, expectedEndDate);
        doThrow(new RuntimeException("database unavailable")).when(pullRerunService)
                .registerFailedPull(eq(DBNAME_1), eq(TeknologiEnum.DB2), any(), any(), any());

        task.pullDB2LogsForAllActiveTasks();

        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(task2, expectedStartDate, expectedEndDate);
    }

    @Test
    void pullDB2LogsForAllActiveTasks_doesNotPullAnythingWhenNoTasksAreActive() {
        enableFeatureFlag();
        grantLock();
        when(auditloggTaskService.findActiveTasksByTeknologi(TeknologiEnum.DB2)).thenReturn(List.of());

        task.pullDB2LogsForAllActiveTasks();

        verify(db2PacketService, never()).fetchLogsWithinDateRangeAndPersistAsPackets(any(), any(), any());
        verify(advisoryLockService).runIfLockAcquired(anyLong(), any(Runnable.class));
    }
}
