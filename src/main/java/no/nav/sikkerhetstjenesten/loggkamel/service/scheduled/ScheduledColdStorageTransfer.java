package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.*;
import no.nav.sikkerhetstjenesten.loggkamel.service.archive.DB2ColdStorageArchiveService;
import no.nav.sikkerhetstjenesten.loggkamel.service.archive.PostgresColdStorageArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class ScheduledColdStorageTransfer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledColdStorageTransfer.class);

    static final String COLD_STORAGE_LOCK_NAME = "loggkamel.cold-storage.scheduled";
    static final long COLD_STORAGE_LOCK_KEY = AdvisoryLockService.lockKeyFromName(COLD_STORAGE_LOCK_NAME);

    private final AuditloggTaskService auditloggTaskService;
    private final DB2ColdStorageArchiveService db2ArchiveService;
    private final PostgresColdStorageArchiveService postgresArchiveService;
    private final GCPColdStorageUploader uploader;
    private final AdvisoryLockService advisoryLockService;
    private final Metrics metrics;

    public ScheduledColdStorageTransfer(
            AuditloggTaskService auditloggTaskService,
            DB2ColdStorageArchiveService db2ArchiveService,
            PostgresColdStorageArchiveService postgresArchiveService,
            GCPColdStorageUploader uploader,
            AdvisoryLockService advisoryLockService,
            Metrics metrics) {
        this.auditloggTaskService = auditloggTaskService;
        this.db2ArchiveService = db2ArchiveService;
        this.postgresArchiveService = postgresArchiveService;
        this.uploader = uploader;
        this.advisoryLockService = advisoryLockService;
        this.metrics = metrics;
    }

    @Scheduled(cron = "${scheduled.cold-storage.cron:-}", zone = "${app.timezone}")
    public void transferRequestedColdStorage() {
        try {
            boolean acquired = advisoryLockService.runIfLockAcquired(
                    COLD_STORAGE_LOCK_KEY, this::transferColdStorageLogsForEligibleTasks);
            if (!acquired) {
                log.info("Another instance holds the cold-storage lock, skipping scheduled cold-storage transfers");
            }
        } catch (RuntimeException exception) {
            log.warn("Scheduled cold-storage run failed while selecting tasks or holding the cold-storage lock ({})",
                    exception.getClass().getSimpleName());
        }
    }

    void transferColdStorageLogsForEligibleTasks() {
        var tasks = auditloggTaskService.findActiveTasksByBackfillStatus(BackfillStatus.COLD_STORAGE_REQUESTED);
        log.info("Starting scheduled cold-storage transfers for {} requested tasks", tasks.size());
        for (AuditloggTaskDTO task : tasks) {
            if (task.getTeknologi() != TeknologiEnum.DB2 && task.getTeknologi() != TeknologiEnum.POSTGRESQL) {
                log.warn("Skipping scheduled cold-storage transfer for unsupported technology {}, database {}",
                        task.getTeknologi(), task.getDbname());
                continue;
            }
            transferColdStorageLogsForGivenTask(task);
        }
    }

    private void transferColdStorageLogsForGivenTask(AuditloggTaskDTO task) {
        String stage = "archive generation";
        int archivesGeneratedCount;
        try (TemporaryLocalArchive archives = switch (task.getTeknologi()) {
            case DB2 -> db2ArchiveService.createLocalArchiveOfColdStorageLogs(task);
            case POSTGRESQL -> postgresArchiveService.createLocalArchiveOfColdStorageLogs(task);
            default -> throw new IllegalStateException("Unsupported cold-storage technology");
        }) {
            archivesGeneratedCount = archives.archivePaths().size();
            stage = "upload";
            uploader.uploadLocalFilesToArchiveBucket(task, archives.archivePaths());
            if (task.getTeknologi() == TeknologiEnum.POSTGRESQL) {
                stage = "source deletion";
                postgresArchiveService.deleteArchivedSourceFiles(task);
            }
            stage = "task completion";
            auditloggTaskService.setBackfillStatus(
                    task.getDbname(), task.getTeknologi(), BackfillStatus.COLD_STORAGE_COMPLETED);
        } catch (RuntimeException exception) {
            log.warn("Scheduled cold-storage transfer failed during {}, database {}; retry on the next run if still requested ({})",
                    stage, task.getDbname(), exception.getClass().getSimpleName());
            metrics.incrementColdStorageFailure();
            return;
        }
        metrics.incrementColdStorageSuccess();
        log.info("Completed scheduled cold-storage transfer, databaseName {}, monthly archive files generated {}",
                task.getDbname(), archivesGeneratedCount);
    }
}
