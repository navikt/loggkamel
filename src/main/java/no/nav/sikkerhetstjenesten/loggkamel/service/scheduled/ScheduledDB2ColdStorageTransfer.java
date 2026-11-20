package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2ColdStorageArchiveService;
import no.nav.sikkerhetstjenesten.loggkamel.service.GCPColdStorageUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class ScheduledDB2ColdStorageTransfer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDB2ColdStorageTransfer.class);

    private final AuditloggTaskService auditloggTaskService;
    private final DB2ColdStorageArchiveService archiveService;
    private final GCPColdStorageUploader uploader;
    private final AdvisoryLockService advisoryLockService;
    private final Metrics metrics;

    public ScheduledDB2ColdStorageTransfer(
            AuditloggTaskService auditloggTaskService,
            DB2ColdStorageArchiveService archiveService,
            GCPColdStorageUploader uploader,
            AdvisoryLockService advisoryLockService,
            Metrics metrics) {
        this.auditloggTaskService = auditloggTaskService;
        this.archiveService = archiveService;
        this.uploader = uploader;
        this.advisoryLockService = advisoryLockService;
        this.metrics = metrics;
    }

    @Scheduled(cron = "${scheduled.db2.cold-storage.cron:-}", zone = "${app.timezone}")
    public void transferRequestedDB2ColdStorage() {
        try {
            boolean acquired = advisoryLockService.runIfLockAcquired(
                    DailyDB2LogPuller.DB2_PULL_LOCK_KEY, this::transferColdStorageLogsForEligibleTasks);
            if (!acquired) {
                log.info("Another instance holds the DB2 pull lock, skipping scheduled DB2 cold-storage transfers");
            }
        } catch (RuntimeException exception) {
            log.warn("Scheduled DB2 cold-storage run failed while selecting tasks or holding the DB2 pull lock ({})",
                    exception.getClass().getSimpleName());
        }
    }

    void transferColdStorageLogsForEligibleTasks() {
        var tasks = auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_REQUESTED);
        log.info("Starting scheduled DB2 cold-storage transfers for {} requested tasks", tasks.size());
        for (AuditloggTaskDTO task : tasks) {
            transferColdStorageLogsForGivenTask(task);
        }
    }

    private void transferColdStorageLogsForGivenTask(AuditloggTaskDTO task) {
        String stage = "archive generation";
        int archivesGeneratedCount;
        try (var archives = archiveService.createLocalArchiveOfColdStorageLogs(task)) {
            archivesGeneratedCount = archives.archivePaths().size();
            stage = "upload";
            uploader.uploadLocalFilesToArchiveBucket(task, archives.archivePaths());
            stage = "task completion";
            auditloggTaskService.setBackfillStatus(
                    task.getDbname(), TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_COMPLETED);
        } catch (RuntimeException exception) {
            log.warn("Scheduled DB2 cold-storage transfer failed during {}, database {}; retry on the next run if still requested ({})",
                    stage, task.getDbname(), exception.getClass().getSimpleName());
            metrics.incrementColdStorageFailure();
            return;
        }
        metrics.incrementColdStorageSuccess();
        log.info("Completed scheduled DB2 cold-storage transfer, databaseName {}, monthly archive files generated {}",
                task.getDbname(), archivesGeneratedCount);
    }
}
