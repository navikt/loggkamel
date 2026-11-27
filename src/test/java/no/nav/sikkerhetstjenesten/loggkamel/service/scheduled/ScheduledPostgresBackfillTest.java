package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import org.junit.jupiter.api.Test;

class ScheduledPostgresBackfillTest {

    @Test
    void scheduledBackfillSkipsProcessingWhenPostgresLockIsHeld() {
    }

    @Test
    void scheduledBackfillUsesLockIndependentOfDB2PullAndColdStorageLocks() {
    }

    @Test
    void scheduledBackfillProcessesTasksWhileHoldingPostgresLock() {
    }

    @Test
    void backfillLogsForAllRequestedTasksPropagatesTaskSelectionFailure() {
    }

    @Test
    void backfillLogsForAllRequestedTasksDoesNotRecordOutcomesWhenNoTasksExist() {
    }

    @Test
    void backfillLogsForAllRequestedTasksContinuesAfterOneTaskFails() {
    }

    @Test
    void backfillLogsForAllRequestedTasksSelectsConfiguredRequestedPostgresTasks() {
    }

    @Test
    void backfillLogsForAllRequestedTasksProcessesEverySelectedTask() {
    }

    @Test
    void backfillSingleTaskLeavesRequestedStatusWhenTransferFails() {
    }

    @Test
    void backfillSingleTaskRecordsFailureWhenCompletionStatusCannotBePersisted() {
    }

    @Test
    void backfillSingleTaskRecordsOneFailureOutcomeForFailedTransfer() {
    }

    @Test
    void backfillSingleTaskCompletesWhenNoFilesMatch() {
    }

    @Test
    void backfillSingleTaskCompletesWhenTransferredSourceWasReplacedByConsumerBackout() {
    }

    @Test
    void backfillSingleTaskCompletesAfterAllFilesHaveBeenMoved() {
    }

    @Test
    void backfillSingleTaskRecordsOneSuccessOutcomeAfterPersistingCompletion() {
    }
}
