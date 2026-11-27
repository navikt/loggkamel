package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import org.junit.jupiter.api.Test;

class ScheduledPostgresBackfillTest {

    @Test
    void scheduledBackfillSkipsProcessingWhenPostgresLockIsHeld() {
    }

    @Test
    void backfillLogsForAllRequestedTasksPropagatesTaskSelectionFailure() {
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
    void backfillSingleTaskCompletesForTaskWithNoHistoricalFilesAwaitingBackfill() {
    }

    @Test
    void backfillSingleTaskCompletesWhenTransferredSourceWasReplacedByConsumerBackout() {
    }

    @Test
    void backfillSingleTaskCompletesAfterAllFilesHaveBeenMoved() {
    }
}
