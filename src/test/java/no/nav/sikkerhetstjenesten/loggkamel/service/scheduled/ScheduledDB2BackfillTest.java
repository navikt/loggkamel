package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import io.getunleash.Unleash;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2PacketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.scheduled.ScheduledDB2Backfill.DB2_BACKFILL_FEATURE_FLAG;
import static no.nav.sikkerhetstjenesten.loggkamel.service.scheduled.DailyDB2LogPuller.DB2_PULL_LOCK_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledDB2BackfillTest {

    private static final String DBNAME_1 = "dbOne";
    private static final String DBNAME_2 = "dbTwo";
    private static final LocalDate JANUARY_FIRST = LocalDate.of(2026, 1, 1);
    private static final LocalDate JANUARY_SECOND = LocalDate.of(2026, 1, 2);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);
    private static final LocalDate START_OF_CURRENT_YEAR = JANUARY_FIRST;
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Mock
    AuditloggTaskService auditloggTaskService;

    @Mock
    DB2PacketService db2PacketService;

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

    @InjectMocks
    ScheduledDB2Backfill backfill;

    @Test
    void scheduledBackfillDoesNothingWhenFeatureFlagIsDisabled() {
        when(unleash.isEnabled(DB2_BACKFILL_FEATURE_FLAG, false)).thenReturn(false);

        backfill.backfillDB2LogsForAllRequestedTasks();

        verifyNoInteractions(advisoryLockService);
        verifyNoInteractions(auditloggTaskService);
        verifyNoInteractions(db2PacketService);
    }

    @Test
    void scheduledBackfillUsesSharedDB2PullLock() {
        when(unleash.isEnabled(DB2_BACKFILL_FEATURE_FLAG, false)).thenReturn(true);
        when(advisoryLockService.runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class))).thenReturn(false);

        backfill.backfillDB2LogsForAllRequestedTasks();

        verify(advisoryLockService).runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class));
        verifyNoInteractions(auditloggTaskService);
        verifyNoInteractions(db2PacketService);
    }

    @Test
    void backfillPullsFromStartOfCurrentYearThroughYesterdayAndMarksTaskFinished() {
        when(task1.getDbname()).thenReturn(DBNAME_1);
        when(auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.DB2, BackfillStatus.REQUESTED)).thenReturn(List.of(task1));

        backfill.backfillLogsForAllRequestedTasks(TODAY);

        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(
                task1, START_OF_CURRENT_YEAR, YESTERDAY);
        verify(auditloggTaskService).setBackfillStatus(
                DBNAME_1, TeknologiEnum.DB2, BackfillStatus.FINISHED);
    }

    @Test
    void backfillOnJanuaryFirstFinishesWithoutPulling() {
        when(task1.getDbname()).thenReturn(DBNAME_1);
        when(auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.DB2, BackfillStatus.REQUESTED)).thenReturn(List.of(task1));

        backfill.backfillLogsForAllRequestedTasks(JANUARY_FIRST);

        verifyNoInteractions(db2PacketService);
        verify(auditloggTaskService).setBackfillStatus(
                DBNAME_1, TeknologiEnum.DB2, BackfillStatus.FINISHED);
    }

    @Test
    void backfillOnJanuarySecondPullsJanuaryFirst() {
        when(task1.getDbname()).thenReturn(DBNAME_1);
        when(auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.DB2, BackfillStatus.REQUESTED)).thenReturn(List.of(task1));

        backfill.backfillLogsForAllRequestedTasks(JANUARY_SECOND);

        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(
                task1, JANUARY_FIRST, JANUARY_FIRST);
        verify(auditloggTaskService).setBackfillStatus(
                DBNAME_1, TeknologiEnum.DB2, BackfillStatus.FINISHED);
    }

    @Test
    void failedBackfillRemainsRequestedAndOtherTasksContinue() {
        when(task1.getDbname()).thenReturn(DBNAME_1);
        when(task2.getDbname()).thenReturn(DBNAME_2);
        when(auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.DB2, BackfillStatus.REQUESTED)).thenReturn(List.of(task1, task2));
        doThrow(new RuntimeException("proxy unavailable"))
                .when(db2PacketService)
                .fetchLogsWithinDateRangeAndPersistAsPackets(
                        task1, START_OF_CURRENT_YEAR, YESTERDAY);

        backfill.backfillLogsForAllRequestedTasks(TODAY);

        verify(auditloggTaskService, never()).setBackfillStatus(
                DBNAME_1, TeknologiEnum.DB2, BackfillStatus.FINISHED);
        verify(metrics).incrementPullFailure(DBNAME_1, TeknologiEnum.DB2);
        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(
                task2, START_OF_CURRENT_YEAR, YESTERDAY);
        verify(auditloggTaskService).setBackfillStatus(
                DBNAME_2, TeknologiEnum.DB2, BackfillStatus.FINISHED);
    }
}
