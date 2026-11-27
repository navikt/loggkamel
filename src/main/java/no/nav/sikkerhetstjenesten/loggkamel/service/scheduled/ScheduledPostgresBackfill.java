package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.PostgresInvalidMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnGCP
public class ScheduledPostgresBackfill {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostgresBackfill.class);
    static final long POSTGRES_BACKFILL_LOCK_KEY = AdvisoryLockService.lockKeyFromName("postgres-backfill");

    private final AuditloggTaskService auditloggTaskService;
    private final PostgresInvalidMessageService invalidMessageService;
    private final AdvisoryLockService advisoryLockService;
    private final Metrics metrics;

    public ScheduledPostgresBackfill(
            AuditloggTaskService auditloggTaskService,
            PostgresInvalidMessageService invalidMessageService,
            AdvisoryLockService advisoryLockService,
            Metrics metrics) {
        this.auditloggTaskService = auditloggTaskService;
        this.invalidMessageService = invalidMessageService;
        this.advisoryLockService = advisoryLockService;
        this.metrics = metrics;
    }

    @Scheduled(cron = "${scheduled.backfill.cron}", zone = "${app.timezone}")
    public void backfillPostgresLogsForAllRequestedTasks() {
        boolean lockAcquired = advisoryLockService.runIfLockAcquired(
                POSTGRES_BACKFILL_LOCK_KEY, this::backfillLogsForAllRequestedTasks);
        if (!lockAcquired) {
            log.info("Another instance holds the Postgres backfill lock, skipping scheduled Postgres backfill");
        }
    }

    void backfillLogsForAllRequestedTasks() {
        List<AuditloggTaskDTO> postgresTasksRequestingBackfill =
                auditloggTaskService.findConfiguredTasksByTeknologiAndBackfillStatus(
                        TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_REQUESTED);

        log.info("Starting scheduled Postgres backfill for {} requested tasks", postgresTasksRequestingBackfill.size());
        int succeeded = 0;
        int failed = 0;
        for (AuditloggTaskDTO task : postgresTasksRequestingBackfill) {
            if (backfillSingleTask(task)) {
                succeeded++;
            } else {
                failed++;
            }
        }
        log.info("Finished scheduled Postgres backfill, tasks succeeded {}, tasks failed {}", succeeded, failed);
    }

    private boolean backfillSingleTask(AuditloggTaskDTO task) {
        try {
            invalidMessageService.moveLogsToConsumerBucket(task);
            auditloggTaskService.setBackfillStatus(
                    task.getDbname(), TeknologiEnum.POSTGRESQL, BackfillStatus.BACKFILL_COMPLETED);
            metrics.incrementBackfillOutcome(TeknologiEnum.POSTGRESQL, Metrics.ActionOutcome.success);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Scheduled Postgres backfill failed, dbname {}; leaving backfill requested",
                    task.getDbname(), exception);
            metrics.incrementBackfillOutcome(TeknologiEnum.POSTGRESQL, Metrics.ActionOutcome.failure);
            return false;
        }
    }
}
