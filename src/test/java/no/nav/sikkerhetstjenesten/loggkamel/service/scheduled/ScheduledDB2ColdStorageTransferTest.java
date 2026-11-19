package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.ColdStorageArchiveException;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2ColdStorageArchiveService;
import no.nav.sikkerhetstjenesten.loggkamel.service.GCPColdStorageUploader;
import no.nav.sikkerhetstjenesten.loggkamel.service.TemporaryLocalArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.scheduled.DailyDB2LogPuller.DB2_PULL_LOCK_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledDB2ColdStorageTransferTest {

    @Mock AuditloggTaskService tasks;
    @Mock
    DB2ColdStorageArchiveService archiveService;
    @Mock GCPColdStorageUploader uploader;
    @Mock AdvisoryLockService locks;
    @Mock Metrics metrics;

    @TempDir Path temp;

    ScheduledDB2ColdStorageTransfer scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ScheduledDB2ColdStorageTransfer(tasks, archiveService, uploader, locks, metrics);
    }

    @Test
    void usesSharedDB2LockAndDoesNothingWhenAlreadyHeld() {
        scheduler.transferRequestedDB2ColdStorage();

        verify(locks).runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class));
        verifyNoInteractions(tasks, archiveService, metrics);
    }

    @Test
    void schedulerInvokesWorkUnderLockWithoutFeatureFlag() {
        givenTasks(List.of());
        when(locks.runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return true;
        });

        scheduler.transferRequestedDB2ColdStorage();

        verify(tasks).findActiveTasksByTeknologiAndBackfillStatus(TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_REQUESTED);
        verifyNoInteractions(metrics);
    }

    @Test
    void completesOnlyAfterAllArchivesAreUploadedAndCleansFiles() throws IOException {
        AuditloggTaskDTO task = task("db");
        givenTasks(List.of(task));
        var archives = archives("db", false);
        when(archiveService.createArchives(task)).thenReturn(archives);

        scheduler.transferColdStorageLogsForEligibleTasks();

        var order = inOrder(archiveService, uploader, tasks, metrics);
        order.verify(archiveService).createArchives(task);
        order.verify(uploader).upload(task, archives.archivePaths());
        order.verify(tasks).setBackfillStatus("db", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
        order.verify(metrics).incrementColdStorageSuccess();
        verifyNoMoreInteractions(metrics);
        assertFalse(Files.exists(archives.temporaryDirectory()));
    }

    @Test
    void failedUploadCountsOnceLeavesTaskRequestedCleansFilesAndContinuesOtherDatabases() throws IOException {
        AuditloggTaskDTO first = task("first");
        AuditloggTaskDTO second = task("second");
        givenTasks(List.of(first, second));
        var firstArchives = archives("first", false);
        var secondArchives = archives("second", false);
        when(archiveService.createArchives(first)).thenReturn(firstArchives);
        when(archiveService.createArchives(second)).thenReturn(secondArchives);
        doThrow(new IllegalStateException("upload failed")).when(uploader).upload(first, firstArchives.archivePaths());

        scheduler.transferColdStorageLogsForEligibleTasks();

        verify(tasks, never()).setBackfillStatus(eq("first"), any(), any());
        verify(tasks).setBackfillStatus("second", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
        verify(metrics).incrementColdStorageFailure();
        verify(metrics).incrementColdStorageSuccess();
        verifyNoMoreInteractions(metrics);
        assertFalse(Files.exists(firstArchives.temporaryDirectory()));
        assertFalse(Files.exists(secondArchives.temporaryDirectory()));
    }

    @Test
    void archiveFailureCountsOnceWithoutUploadingOrCompleting() {
        AuditloggTaskDTO task = task("db");
        givenTasks(List.of(task));
        when(archiveService.createArchives(task)).thenThrow(new ColdStorageArchiveException("generation failed", new IOException()));

        scheduler.transferColdStorageLogsForEligibleTasks();

        verifyNoInteractions(uploader);
        verify(tasks, never()).setBackfillStatus(anyString(), any(), any());
        verify(metrics).incrementColdStorageFailure();
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void failuresAreCountedOncePerTaskOnEachRun() {
        AuditloggTaskDTO first = task("first");
        AuditloggTaskDTO second = task("second");
        givenTasks(List.of(first, second));
        when(archiveService.createArchives(any(AuditloggTaskDTO.class)))
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
        AuditloggTaskDTO task = task("db");
        givenTasks(List.of(task));
        when(archiveService.createArchives(task)).thenReturn(archives("db", true));

        scheduler.transferColdStorageLogsForEligibleTasks();

        verify(uploader).upload(task, List.of());
        verify(tasks).setBackfillStatus("db", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
        verify(metrics).incrementColdStorageSuccess();
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void completionDatabaseFailureCountsOnceAndCleansFiles() throws IOException {
        AuditloggTaskDTO task = task("db");
        givenTasks(List.of(task));
        var archives = archives("db", false);
        when(archiveService.createArchives(task)).thenReturn(archives);
        doThrow(new IllegalStateException("database unavailable")).when(tasks)
                .setBackfillStatus("db", TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);

        scheduler.transferColdStorageLogsForEligibleTasks();

        verify(metrics).incrementColdStorageFailure();
        verifyNoMoreInteractions(metrics);
        assertFalse(Files.exists(archives.temporaryDirectory()));
    }

    @Test
    void cleanupFailureDoesNotTurnSuccessfulPublicationIntoTransferFailure() throws IOException {
        AuditloggTaskDTO task = task("db");
        givenTasks(List.of(task));
        var archives = archives("db", false);
        when(archiveService.createArchives(task)).thenReturn(archives);
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
        when(locks.runIfLockAcquired(eq(DB2_PULL_LOCK_KEY), any(Runnable.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(scheduler::transferRequestedDB2ColdStorage);
        verifyNoInteractions(metrics);
    }

    @Test
    void cronAndTimezoneFollowEnvironmentConfiguration() throws NoSuchMethodException {
        Scheduled scheduled = ScheduledDB2ColdStorageTransfer.class
                .getMethod("transferRequestedDB2ColdStorage").getAnnotation(Scheduled.class);
        assertEquals("${app.timezone}", scheduled.zone());
        assertEquals("${scheduled.db2.cold-storage.cron:-}", scheduled.cron());
        assertEquals("0 */10 * * * *", properties("application-dev-gcp.yaml")
                .getProperty("scheduled.db2.cold-storage.cron"));
        assertEquals("0 30 3 * * *", properties("application-prod-gcp.yaml")
                .getProperty("scheduled.db2.cold-storage.cron"));
    }

    private void givenTasks(List<AuditloggTaskDTO> requested) {
        when(tasks.findActiveTasksByTeknologiAndBackfillStatus(TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_REQUESTED))
                .thenReturn(requested);
    }

    private TemporaryLocalArchive archives(String db, boolean empty) throws IOException {
        Path directory = Files.createDirectory(temp.resolve(db));
        List<Path> paths = empty ? List.of() : List.of(
                Files.createFile(directory.resolve("202601-loggkamel-" + db + ".zip")),
                Files.createFile(directory.resolve("202602-loggkamel-" + db + ".zip")));
        return new TemporaryLocalArchive(directory, paths);
    }

    private static AuditloggTaskDTO task(String db) {
        AuditloggTaskDTO task = mock(AuditloggTaskDTO.class);
        when(task.getDbname()).thenReturn(db);
        return task;
    }

    private static java.util.Properties properties(String resource) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resource));
        return factory.getObject();
    }
}
