package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.IdempotentMessageRepository;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.LOG_PACKET_CONSUMER;
import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.POSTGRES_CONSUMER;

@Service
public class IdempotentRepositoryCleanupService {

    private static final Logger log = LoggerFactory.getLogger(IdempotentRepositoryCleanupService.class);

    static final long CLEANUP_LOCK_KEY = AdvisoryLockService.lockKeyFromName("loggkamel.idempotent-repository.cleanup.scheduled");

    private final IdempotentMessageRepository idempotentMessageRepository;
    private final AdvisoryLockService advisoryLockService;
    final Duration postgresLockRetention;
    final Duration packetLockRetention;

    @Autowired
    public IdempotentRepositoryCleanupService(IdempotentMessageRepository idempotentMessageRepository,
                                              AdvisoryLockService advisoryLockService,
                                              @Value("${scheduled.idempotent-repository.cleanup.postgres-retention-minutes}") long postgresLockRetentionMinutes,
                                              @Value("${scheduled.idempotent-repository.cleanup.log-packet-retention-minutes}") long packetLockRetentionMinutes) {
        this.idempotentMessageRepository = idempotentMessageRepository;
        this.advisoryLockService = advisoryLockService;
        this.postgresLockRetention = Duration.ofMinutes(postgresLockRetentionMinutes);
        this.packetLockRetention = Duration.ofMinutes(packetLockRetentionMinutes);
    }

    @Scheduled(cron = "${scheduled.idempotent-repository.cleanup.cron}", zone = "${app.timezone}")
    public void cleanupExpiredLocks() {
        boolean lockAcquired = advisoryLockService.runIfLockAcquired(CLEANUP_LOCK_KEY, this::deleteExpiredLocks);

        if (!lockAcquired) {
            log.info("Another instance holds the idempotent repository cleanup lock, skipping scheduled cleanup");
        }
    }

    private void deleteExpiredLocks() {
        Instant now = Instant.now();

        int postgresEntriesDeleted = idempotentMessageRepository.deleteMessagesOlderThan(
                POSTGRES_CONSUMER, now.minus(postgresLockRetention));
        int logPacketEntriesDeleted = idempotentMessageRepository.deleteMessagesOlderThan(
                LOG_PACKET_CONSUMER, now.minus(packetLockRetention));

        log.info("Idempotent repository cleanup complete, postgres entries deleted {}, log packet entries deleted {}",
                postgresEntriesDeleted, logPacketEntriesDeleted);
    }
}
