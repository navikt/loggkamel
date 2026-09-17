package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.IdempotentMessageRepository;
import no.nav.sikkerhetstjenesten.loggkamel.service.AdvisoryLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.LOG_PACKET_CONSUMER;
import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.POSTGRES_CONSUMER;
import static no.nav.sikkerhetstjenesten.loggkamel.service.scheduled.IdempotentRepositoryCleanupService.CLEANUP_LOCK_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentRepositoryCleanupServiceTest {

    @Mock
    IdempotentMessageRepository idempotentMessageRepository;

    @Mock
    AdvisoryLockService advisoryLockService;

    IdempotentRepositoryCleanupService service;

    @BeforeEach
    void setUp() {
        service = new IdempotentRepositoryCleanupService(
                idempotentMessageRepository,
                advisoryLockService,
                360,
                10);
    }

    @Test
    void cleanupExpiredLocks_deletesMessagesOlderThanRetentionWhenLockIsAcquired() {
        when(advisoryLockService.runIfLockAcquired(eq(CLEANUP_LOCK_KEY), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(1, Runnable.class).run();
                    return true;
                });
        service.cleanupExpiredLocks();

        Instant cleanupCompleted = Instant.now();
        ArgumentCaptor<Instant> postgresCutoff = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> logPacketCutoff = ArgumentCaptor.forClass(Instant.class);
        verify(idempotentMessageRepository)
                .deleteMessagesOlderThan(eq(POSTGRES_CONSUMER), postgresCutoff.capture());
        verify(idempotentMessageRepository)
                .deleteMessagesOlderThan(eq(LOG_PACKET_CONSUMER), logPacketCutoff.capture());

        assertCapturedCutoffWithinASecondOfConfiguredRetention(postgresCutoff.getValue(), cleanupCompleted, service.postgresLockRetention);
        assertCapturedCutoffWithinASecondOfConfiguredRetention(logPacketCutoff.getValue(), cleanupCompleted, service.packetLockRetention);
    }

    @Test
    void cleanupExpiredLocks_doesNotDeleteWhenLockIsHeldByAnotherInstance() {
        when(advisoryLockService.runIfLockAcquired(eq(CLEANUP_LOCK_KEY), any(Runnable.class))).thenReturn(false);

        service.cleanupExpiredLocks();

        verifyNoInteractions(idempotentMessageRepository);
    }

    private void assertCapturedCutoffWithinASecondOfConfiguredRetention(Instant cutoff, Instant cleanupCompleted, Duration expectedRetention) {
        assertEquals(expectedRetention.toMillis(), Duration.between(cutoff, cleanupCompleted).toMillis(), 1_000);
    }
}
