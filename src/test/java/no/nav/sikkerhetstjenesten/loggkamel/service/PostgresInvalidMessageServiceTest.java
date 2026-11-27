package no.nav.sikkerhetstjenesten.loggkamel.service;

import org.junit.jupiter.api.Test;

class PostgresInvalidMessageServiceTest {

    @Test
    void streamLogFilesForTaskPropagatesListingFailure() {
    }

    @Test
    void streamLogFilesForTaskReturnsEmptyStreamWhenNoObjectsMatch() {
    }

    @Test
    void streamLogFilesForTaskExcludesOtherDatabasePrefixes() {
    }

    @Test
    void streamLogFilesForTaskExcludesMalformedFilenames() {
    }

    @Test
    void streamLogFilesForTaskReadsAllListingPages() {
    }

    @Test
    void moveLogsToConsumerBucketPropagatesInitialListingFailure() {
    }

    @Test
    void moveLogsToConsumerBucketCompletesWithoutCopyingWhenNoFilesMatch() {
    }

    @Test
    void moveLogsToConsumerBucketDoesNotDeleteSourceWhenCopyStartFails() {
    }

    @Test
    void moveLogsToConsumerBucketDoesNotDeleteSourceWhenCopyCompletionFails() {
    }

    @Test
    void moveLogsToConsumerBucketPropagatesFailureWhileReadingLaterListingPage() {
    }

    @Test
    void moveLogsToConsumerBucketOverwritesExistingDestinationObject() {
    }

    @Test
    void moveLogsToConsumerBucketWaitsForCopyConfirmationBeforeDeletingSource() {
    }

    @Test
    void moveLogsToConsumerBucketMovesAllMatchingFilesAcrossListingPages() {
    }

    @Test
    void deleteTransferredSourcePropagatesDeletionErrorsOtherThanGenerationMismatch() {
    }

    @Test
    void deleteTransferredSourceFailsWhenStorageDoesNotConfirmDeletion() {
    }

    @Test
    void deleteTransferredSourcePreservesReplacementGenerationWithoutFailingTransfer() {
    }
}
