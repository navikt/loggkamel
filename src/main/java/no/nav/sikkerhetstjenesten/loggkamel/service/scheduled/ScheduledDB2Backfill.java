package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@ConditionalOnGCP
public class ScheduledDB2Backfill {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDB2Backfill.class);
    static final long DB2_BACKFILL_LOCK_KEY = AdvisoryLockService.lockKeyFromName("db2-backfill");

    private final AuditloggTaskService auditloggTaskService;
    private final DB2PacketService db2PacketService;
    private final AdvisoryLockService advisoryLockService;
    private final Metrics metrics;
    private final ZoneId appZoneId;

    @Autowired
    public ScheduledDB2Backfill(
            AuditloggTaskService auditloggTaskService,
            DB2PacketService db2PacketService,
            AdvisoryLockService advisoryLockService,
            Metrics metrics,
            ZoneId appZoneId) {
        this.auditloggTaskService = auditloggTaskService;
        this.db2PacketService = db2PacketService;
        this.advisoryLockService = advisoryLockService;
        this.metrics = metrics;
        this.appZoneId = appZoneId;
    }

    @Scheduled(cron = "${scheduled.backfill.cron}", zone = "${app.timezone}")
    public void backfillDB2LogsForAllRequestedTasks() {
        boolean lockAcquired = advisoryLockService.runIfLockAcquired(
                DB2_BACKFILL_LOCK_KEY,
                () -> backfillLogsForAllRequestedTasks(LocalDate.now(appZoneId)));

        if (!lockAcquired) {
            log.info("Another instance holds the DB2 backfill lock, skipping scheduled DB2 backfill");
        }
    }

    void backfillLogsForAllRequestedTasks(LocalDate today) {
        LocalDate startOfYear = today.withDayOfYear(1);
        LocalDate yesterday = today.minusDays(1);
        List<AuditloggTaskDTO> requestedTasks =
                auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                        TeknologiEnum.DB2, BackfillStatus.BACKFILL_REQUESTED);

        log.info("Starting scheduled DB2 backfill for {} requested tasks, startDate {}, endDate {}",
                requestedTasks.size(), startOfYear, yesterday);

        int succeeded = 0;
        int failed = 0;

        for (AuditloggTaskDTO requestedTask : requestedTasks) {
            if (backfillSingleTask(requestedTask, startOfYear, yesterday)) {
                succeeded++;
            } else {
                failed++;
            }
        }

        log.info("Finished scheduled DB2 backfill, startDate {}, endDate {}, tasks succeeded {}, tasks failed {}",
                startOfYear, yesterday, succeeded, failed);
    }

    private boolean backfillSingleTask(
            AuditloggTaskDTO requestedTask, LocalDate pullStartDate, LocalDate pullEndDate) {
        try {
            if (!pullEndDate.isBefore(pullStartDate)) {
                db2PacketService.fetchLogsWithinDateRangeAndPersistAsPackets(
                        requestedTask, pullStartDate, pullEndDate);
            }

            auditloggTaskService.setBackfillStatus(
                    requestedTask.getDbname(),
                    TeknologiEnum.DB2,
                    BackfillStatus.BACKFILL_COMPLETED);
            metrics.incrementBackfillOutcome(TeknologiEnum.DB2, Metrics.ActionOutcome.success);
            return true;
        } catch (RuntimeException e) {
            log.warn("Scheduled DB2 backfill failed, dbname {}, startDate {}, endDate {}; leaving backfill requested",
                    requestedTask.getDbname(), pullStartDate, pullEndDate, e);
            metrics.incrementPullFailure(requestedTask.getDbname(), TeknologiEnum.DB2);
            metrics.incrementBackfillOutcome(TeknologiEnum.DB2, Metrics.ActionOutcome.failure);
            return false;
        }
    }
}
