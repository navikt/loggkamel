package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import io.getunleash.Unleash;
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
public class DailyDB2Backfill {

    private static final Logger log = LoggerFactory.getLogger(DailyDB2Backfill.class);
    private static final ZoneId SCHEDULER_ZONE = ZoneId.of("Europe/Oslo");

    static final String DB2_BACKFILL_FEATURE_FLAG = "backfill-db2-logs";

    private final AuditloggTaskService auditloggTaskService;
    private final DB2PacketService db2PacketService;
    private final AdvisoryLockService advisoryLockService;
    private final Metrics metrics;
    private final Unleash unleash;

    @Autowired
    public DailyDB2Backfill(
            AuditloggTaskService auditloggTaskService,
            DB2PacketService db2PacketService,
            AdvisoryLockService advisoryLockService,
            Metrics metrics,
            Unleash unleash) {
        this.auditloggTaskService = auditloggTaskService;
        this.db2PacketService = db2PacketService;
        this.advisoryLockService = advisoryLockService;
        this.metrics = metrics;
        this.unleash = unleash;
    }

    @Scheduled(cron = "${scheduled.db2.backfill.cron}", zone = "Europe/Oslo")
    public void backfillDB2LogsForAllRequestedTasks() {
        if (!unleash.isEnabled(DB2_BACKFILL_FEATURE_FLAG, false)) {
            log.info("Feature flag '{}' is disabled, skipping scheduled DB2 backfill", DB2_BACKFILL_FEATURE_FLAG);
            return;
        }

        boolean lockAcquired = advisoryLockService.runIfLockAcquired(
                DailyDB2LogPuller.DB2_PULL_LOCK_KEY,
                () -> backfillLogsForAllRequestedTasks(LocalDate.now(SCHEDULER_ZONE)));

        if (!lockAcquired) {
            log.info("Another instance holds the DB2 pull lock, skipping scheduled DB2 backfill");
        }
    }

    void backfillLogsForAllRequestedTasks(LocalDate today) {
        LocalDate pullStartDate = today.withDayOfYear(1);
        LocalDate pullEndDate = today.minusDays(1);
        List<AuditloggTaskDTO> requestedTasks =
                auditloggTaskService.findActiveTasksByTeknologiAndBackfillStatus(
                        TeknologiEnum.DB2, BackfillStatus.REQUESTED);

        log.info("Starting scheduled DB2 backfill for {} requested tasks, startDate {}, endDate {}",
                requestedTasks.size(), pullStartDate, pullEndDate);

        int succeeded = 0;
        int failed = 0;

        for (AuditloggTaskDTO requestedTask : requestedTasks) {
            if (backfillSingleTask(requestedTask, pullStartDate, pullEndDate)) {
                succeeded++;
            } else {
                failed++;
            }
        }

        log.info("Finished scheduled DB2 backfill, startDate {}, endDate {}, tasks succeeded {}, tasks failed {}",
                pullStartDate, pullEndDate, succeeded, failed);
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
                    BackfillStatus.FINISHED);
            return true;
        } catch (RuntimeException e) {
            log.warn("Scheduled DB2 backfill failed, dbname {}, startDate {}, endDate {}; leaving backfill requested",
                    requestedTask.getDbname(), pullStartDate, pullEndDate, e);
            metrics.incrementPullFailure(requestedTask.getDbname(), TeknologiEnum.DB2);
            return false;
        }
    }
}
