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
    void streamLogFilesForTaskIncludesUncompressedAuditlogs() {
    }

    @Test
    void streamLogFilesForTaskIncludesGzipAuditlogs() {
    }

    @Test
    void streamLogFilesForTaskIncludesAllDatesWithoutBackfillWindow() {
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
    void moveLogsToConsumerBucketStopsAtFailedFileWithoutUndoingEarlierMoves() {
    }

    @Test
    void moveLogsToConsumerBucketPropagatesFailureWhileReadingLaterListingPage() {
    }

    @Test
    void moveLogsToConsumerBucketCopiesListedSourceGeneration() {
    }

    @Test
    void moveLogsToConsumerBucketOverwritesExistingDestinationObject() {
    }

    @Test
    void moveLogsToConsumerBucketPreservesFilenameAndContents() {
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

    @Test
    void deleteTransferredSourceDeletesOnlyTheGenerationThatWasCopied() {
    }
}
