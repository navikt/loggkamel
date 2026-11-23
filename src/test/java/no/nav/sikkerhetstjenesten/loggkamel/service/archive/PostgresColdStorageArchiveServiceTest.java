package no.nav.sikkerhetstjenesten.loggkamel.service.archive;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PostgresColdStorageArchiveServiceTest {

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveMatchesExactDatabasePrefix() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveAllowsLiteralRegexCharactersInDatabaseName() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveAcceptsPlainAuditlogSuffix() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveAcceptsGzipAuditlogSuffix() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveDoesNotRestrictDates() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveReadsAllListingPages() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveGroupsFilesByYearAndMonth() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveSortsFilesAcrossListingPagesBeforeGroupingByMonth() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchivePreservesSourceFilenamesInZip() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchivePreservesRawPlainFileBytes() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchivePreservesGzipFileBytes() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deleteArchivedSourceFilesRelistsExactDatabaseObjects() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deleteArchivedSourceFilesProcessesAllListingPages() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveReadsObjectsWithoutGenerations() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deleteArchivedSourceFilesDeletesObjectsWithoutGenerations() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deleteArchivedSourceFilesAcceptsAlreadyMissingObjects() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deleteArchivedSourceFilesStopsWhenDeletionFails() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveFailsWhenSourceListingFails() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deleteArchivedSourceFilesFailsWhenSourceListingFails() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveStreamsLargeObjectsBoundedly() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveHandlesEmptySourceSet() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveCleansTemporaryDirectoryWhenCreationFails() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveRejectsNullTask() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveRejectsNonPostgresTask() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveRejectsTaskOutsideColdStorageRequestedState() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void createLocalArchiveRejectsInvalidDatabaseName() {
    }
}
