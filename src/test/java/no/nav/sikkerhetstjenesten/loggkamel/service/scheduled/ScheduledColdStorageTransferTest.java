package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.archive.DB2ColdStorageArchiveService;
import no.nav.sikkerhetstjenesten.loggkamel.service.GCPColdStorageUploader;
import no.nav.sikkerhetstjenesten.loggkamel.service.archive.PostgresColdStorageArchiveService;
import no.nav.sikkerhetstjenesten.loggkamel.service.TemporaryLocalArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.scheduled.ScheduledColdStorageTransfer.COLD_STORAGE_LOCK_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledColdStorageTransferTest {

    private static final String DB_NAME_1 = "dbName1";
    private static final String DB_NAME_2 = "dbName2";

    @Mock
    AuditloggTaskService tasks;

    @Mock
    DB2ColdStorageArchiveService archiveService;

    @Mock
    PostgresColdStorageArchiveService postgresArchiveService;

    @Mock
    GCPColdStorageUploader uploader;

    @Mock
    AdvisoryLockService locks;

    @Mock
    Metrics metrics;

    @TempDir
    Path temp;

    @InjectMocks
    ScheduledColdStorageTransfer scheduler;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO1;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO2;

    @BeforeEach
    void setUp() {
        lenient().when(auditloggTaskDTO1.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        lenient().when(auditloggTaskDTO2.getTeknologi()).thenReturn(TeknologiEnum.DB2);
    }

    @Test
    void usesDedicatedColdStorageLockAndDoesNothingWhenAlreadyHeld() {
        scheduler.transferRequestedColdStorage();

        verify(locks).runIfLockAcquired(eq(COLD_STORAGE_LOCK_KEY), any(Runnable.class));
        verifyNoInteractions(tasks, archiveService, metrics);
    }

    @Test
    void emptyTaskListDoesNothing() {
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of());
        when(locks.runIfLockAcquired(eq(COLD_STORAGE_LOCK_KEY), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return true;
        });

        scheduler.transferRequestedColdStorage();

        verify(tasks).findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED);
        verifyNoInteractions(metrics);
    }

    @Test
    void completesOnlyAfterAllArchivesAreUploadedAndCleansFiles() throws IOException {
        when(auditloggTaskDTO1.getDbname()).thenReturn(DB_NAME_1);
        when(auditloggTaskDTO1.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of(auditloggTaskDTO1));

        var archives = archives(DB_NAME_1, false);
        when(archiveService.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO1)).thenReturn(archives);

        scheduler.transferColdStorageLogsForEligibleTasks();

        var order = inOrder(archiveService, uploader, tasks, metrics);
        order.verify(archiveService).createLocalArchiveOfColdStorageLogs(auditloggTaskDTO1);
        order.verify(uploader).uploadLocalFilesToArchiveBucket(auditloggTaskDTO1, archives.archivePaths());
        order.verify(tasks).setBackfillStatus(DB_NAME_1, TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
        order.verify(metrics).incrementColdStorageSuccess();
        verifyNoMoreInteractions(metrics);
        assertFalse(Files.exists(archives.temporaryDirectory()));
    }

    @Test
    void failedUploadLeavesTaskRequestedCleansFilesAndContinuesOtherDatabases() throws IOException {
        when(auditloggTaskDTO1.getDbname()).thenReturn(DB_NAME_1);
        when(auditloggTaskDTO2.getDbname()).thenReturn(DB_NAME_2);
        when(auditloggTaskDTO2.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of(auditloggTaskDTO1, auditloggTaskDTO2));

        var firstArchives = archives(DB_NAME_1, false);
        var secondArchives = archives(DB_NAME_2, false);
        when(archiveService.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO1)).thenReturn(firstArchives);
        when(archiveService.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO2)).thenReturn(secondArchives);
        doThrow(new IllegalStateException("upload failed")).when(uploader).uploadLocalFilesToArchiveBucket(auditloggTaskDTO1, firstArchives.archivePaths());

        scheduler.transferColdStorageLogsForEligibleTasks();

        verify(tasks, never()).setBackfillStatus(eq(DB_NAME_1), any(), any());
        verify(tasks).setBackfillStatus(DB_NAME_2, TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);

        verify(metrics).incrementColdStorageFailure();
        verify(metrics).incrementColdStorageSuccess();
        verifyNoMoreInteractions(metrics);

        assertFalse(Files.exists(firstArchives.temporaryDirectory()));
        assertFalse(Files.exists(secondArchives.temporaryDirectory()));
    }

    @Test
    void archiveFailureCountsOnceWithoutUploadingOrCompleting() {
        when(auditloggTaskDTO1.getDbname()).thenReturn(DB_NAME_1);
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of(auditloggTaskDTO1));

        when(archiveService.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO1))
                .thenThrow(new IllegalStateException("generation failed", new IOException()));

        scheduler.transferColdStorageLogsForEligibleTasks();

        verifyNoInteractions(uploader);
        verify(tasks, never()).setBackfillStatus(anyString(), any(), any());
        verify(metrics).incrementColdStorageFailure();
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void failuresAreCountedOncePerTaskOnEachRun() {
        when(auditloggTaskDTO1.getDbname()).thenReturn(DB_NAME_1);
        when(auditloggTaskDTO2.getDbname()).thenReturn(DB_NAME_2);
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of(auditloggTaskDTO1, auditloggTaskDTO2));

        when(archiveService.createLocalArchiveOfColdStorageLogs(any(AuditloggTaskDTO.class)))
                .thenThrow(new IllegalStateException("proxy unavailable"));

        scheduler.transferColdStorageLogsForEligibleTasks();
        verify(metrics, times(2)).incrementColdStorageFailure();

        scheduler.transferColdStorageLogsForEligibleTasks();
        verify(metrics, times(4)).incrementColdStorageFailure();

        verifyNoMoreInteractions(metrics);
        verifyNoInteractions(uploader);
        verify(tasks, never()).setBackfillStatus(anyString(), any(), any());
    }

    @Test
    void emptyExportStillCompletesSuccessfully() throws IOException {
        when(auditloggTaskDTO1.getDbname()).thenReturn(DB_NAME_1);
        when(auditloggTaskDTO1.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of(auditloggTaskDTO1));

        when(archiveService.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO1)).thenReturn(archives(DB_NAME_1, true));

        scheduler.transferColdStorageLogsForEligibleTasks();

        verify(uploader).uploadLocalFilesToArchiveBucket(auditloggTaskDTO1, List.of());
        verify(tasks).setBackfillStatus(DB_NAME_1, TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
        verify(metrics).incrementColdStorageSuccess();
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void cleanupFailureDoesNotTurnSuccessfulPublicationIntoTransferFailure() throws IOException {
        when(auditloggTaskDTO1.getDbname()).thenReturn(DB_NAME_1);
        when(tasks.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(List.of(auditloggTaskDTO1));

        var archives = archives(DB_NAME_1, false);
        when(archiveService.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO1)).thenReturn(archives);
        try (var files = mockStatic(Files.class)) {
            files.when(() -> Files.walkFileTree(eq(archives.temporaryDirectory()), any()))
                    .thenThrow(new IOException("disk failure"));

            scheduler.transferColdStorageLogsForEligibleTasks();

            verify(metrics).incrementColdStorageSuccess();
            verifyNoMoreInteractions(metrics);
        }
    }

    @Test
    void runLevelFailureIsWarnedWithoutInventingTaskFailures() {
        when(locks.runIfLockAcquired(eq(COLD_STORAGE_LOCK_KEY), any(Runnable.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(scheduler::transferRequestedColdStorage);
        verifyNoInteractions(metrics);
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void routesPostgresTasksToPostgresArchiveStrategy() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void passesPostgresTaskToSourceDeletion() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void uploadsAllArchivesBeforeDeletingAnySource() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void doesNotDeleteSourcesWhenArchiveGenerationFails() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void doesNotDeleteSourcesWhenUploadFails() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deletionFailureLeavesTaskRequested() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deletionFailureAllowsNextTask() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void deletionFailureCountsOneTaskFailure() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void updatesTaskStatusOnlyAfterSourceDeletion() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void completesTaskWhenNoPostgresSourcesMatch() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void acceptsExistingDestinationBeforeDeletingSources() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void skipsUnsupportedTechnologyWithoutChangingTaskStatus() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void unsupportedTechnologyDoesNotIncrementSuccessMetric() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void unsupportedTechnologyLogsWarning() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void usesColdStorageLockDistinctFromDb2PullLock() {
    }

    @Test
    @Disabled("Pending coverage review and test implementation")
    void taskCompletionFailureLeavesTaskRequested() {
    }

    private TemporaryLocalArchive archives(String db, boolean empty) throws IOException {
        Path directory = Files.createDirectory(temp.resolve(db));
        List<Path> paths = empty ? List.of() : List.of(
                Files.createFile(directory.resolve("202601-loggkamel-" + db + ".zip")),
                Files.createFile(directory.resolve("202602-loggkamel-" + db + ".zip")));
        return new TemporaryLocalArchive(directory, paths);
    }

}
