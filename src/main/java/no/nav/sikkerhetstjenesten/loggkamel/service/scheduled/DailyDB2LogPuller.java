package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import io.getunleash.Unleash;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2PacketService;
import no.nav.sikkerhetstjenesten.loggkamel.service.PullRerunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DailyDB2LogPuller {

    private static final Logger log = LoggerFactory.getLogger(DailyDB2LogPuller.class);

    static final String DB2_PULL_LOCK_NAME = "loggkamel.pull.db2.scheduled";
    static final long DB2_PULL_LOCK_KEY = AdvisoryLockService.lockKeyFromName(DB2_PULL_LOCK_NAME);

    static final String DB2_PULL_FEATURE_FLAG = "pull-db2-logs";

    static final int PULL_START_DAYS_AGO = 2;
    static final int PULL_END_DAYS_AGO = 1;

    private final AuditloggTaskService auditloggTaskService;
    private final DB2PacketService db2PacketService;
    private final PullRerunService pullRerunService;
    private final AdvisoryLockService advisoryLockService;
    private final Metrics metrics;
    private final Unleash unleash;

    @Autowired
    public DailyDB2LogPuller(AuditloggTaskService auditloggTaskService,
                             DB2PacketService db2PacketService,
                             PullRerunService pullRerunService,
                             AdvisoryLockService advisoryLockService,
                             Metrics metrics,
                             Unleash unleash) {
        this.auditloggTaskService = auditloggTaskService;
        this.db2PacketService = db2PacketService;
        this.pullRerunService = pullRerunService;
        this.advisoryLockService = advisoryLockService;
        this.metrics = metrics;
        this.unleash = unleash;
    }

    @Scheduled(cron = "${scheduled.db2.pull.cron}", zone = "${app.timezone}")
    public void pullDB2LogsForAllActiveTasks() {
        if (!unleash.isEnabled(DB2_PULL_FEATURE_FLAG, false)) {
            log.info("Feature flag '{}' is disabled, skipping scheduled DB2 log pull", DB2_PULL_FEATURE_FLAG);
            return;
        }

        boolean lockAcquired = advisoryLockService.runIfLockAcquired(DB2_PULL_LOCK_KEY, this::pullLogsForAllActiveTasks);

        if (!lockAcquired) {
            log.info("Another instance holds the DB2 pull lock, skipping scheduled DB2 log pull");
        }
    }

    private void pullLogsForAllActiveTasks() {
        LocalDate today = LocalDate.now();
        LocalDate pullStartDate = today.minusDays(PULL_START_DAYS_AGO);
        LocalDate pullEndDate = today.minusDays(PULL_END_DAYS_AGO);

        List<AuditloggTaskDTO> activeTasks = auditloggTaskService.findActiveTasksByTeknologi(TeknologiEnum.DB2);
        log.info("Starting scheduled DB2 log pull for {} active tasks, startDate {}, endDate {}", activeTasks.size(), pullStartDate, pullEndDate);

        int succeeded = 0;
        int failed = 0;

        for (AuditloggTaskDTO activeTask : activeTasks) {
            if (pullLogsForSingleTask(activeTask, pullStartDate, pullEndDate)) {
                succeeded++;
            } else {
                failed++;
            }
        }

        log.info("Finished scheduled DB2 log pull, startDate {}, endDate {}, tasks succeeded {}, tasks failed {}",
                pullStartDate, pullEndDate, succeeded, failed);
    }

    private boolean pullLogsForSingleTask(AuditloggTaskDTO activeTask, LocalDate pullStartDate, LocalDate pullEndDate) {
        try {
            db2PacketService.fetchLogsWithinDateRangeAndPersistAsPackets(activeTask, pullStartDate, pullEndDate);
            return true;
        } catch (Exception e) {
            log.warn("Scheduled DB2 log pull failed, dbname {}, startDate {}, endDate {}, registering for manual rerun",
                    activeTask.getDbname(), pullStartDate, pullEndDate, e);
            metrics.incrementPullFailure(activeTask.getDbname(), TeknologiEnum.DB2);
            registerFailedPull(activeTask, pullStartDate, pullEndDate, e);
            return false;
        }
    }

    private void registerFailedPull(AuditloggTaskDTO activeTask, LocalDate pullStartDate, LocalDate pullEndDate, Exception cause) {
        try {
            pullRerunService.registerFailedPull(activeTask.getDbname(), TeknologiEnum.DB2, pullStartDate, pullEndDate, cause);
        } catch (Exception registrationException) {
            // Losing this record means the failed range has no trace other than logs and metrics, so it is logged loudly
            log.error("Failed to register DB2 log pull failure for manual rerun, dbname {}, startDate {}, endDate {}",
                    activeTask.getDbname(), pullStartDate, pullEndDate, registrationException);
        }
    }
}
