package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.testsupport.DockerAvailableCondition;
import no.nav.sikkerhetstjenesten.loggkamel.testsupport.SharedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the real Postgres advisory lock semantics with two concurrent callers. */
@ExtendWith(DockerAvailableCondition.class)
class AdvisoryLockServicePostgresTest {

    private static final long TIMEOUT_SECONDS = 10;

    private AdvisoryLockService service;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        service = new AdvisoryLockService(
                new DriverManagerDataSource(SharedPostgres.jdbcUrl(), SharedPostgres.username(), SharedPostgres.password()));
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void secondCallerWithSameKeyIsRejectedWhileLockIsHeld() throws Exception {
        long key = AdvisoryLockService.lockKeyFromName("test.same-key");
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<Boolean> holder = executor.submit(() -> service.runIfLockAcquired(key, () -> {
            holding.countDown();
            awaitQuietly(release);
        }));
        assertThat(holding.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        AtomicBoolean secondRan = new AtomicBoolean();

        boolean secondAcquired = service.runIfLockAcquired(key, () -> secondRan.set(true));

        assertThat(secondAcquired).isFalse();
        assertThat(secondRan).isFalse();
        release.countDown();
        assertThat(holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void differentKeysDoNotBlockEachOther() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<Boolean> holder = executor.submit(() -> service.runIfLockAcquired(
                AdvisoryLockService.lockKeyFromName("test.key-one"), () -> {
                    holding.countDown();
                    awaitQuietly(release);
                }));
        assertThat(holding.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        AtomicBoolean otherRan = new AtomicBoolean();

        boolean acquired = service.runIfLockAcquired(AdvisoryLockService.lockKeyFromName("test.key-two"), () -> otherRan.set(true));

        assertThat(acquired).isTrue();
        assertThat(otherRan).isTrue();
        release.countDown();
        holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void lockIsReleasedAfterActionCompletes() {
        long key = AdvisoryLockService.lockKeyFromName("test.release-after-complete");

        assertThat(service.runIfLockAcquired(key, () -> { })).isTrue();
        assertThat(service.runIfLockAcquired(key, () -> { })).isTrue();
    }

    @Test
    void lockIsReleasedWhenActionThrows() {
        long key = AdvisoryLockService.lockKeyFromName("test.release-after-failure");

        assertThatThrownBy(() -> service.runIfLockAcquired(key, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");

        assertThat(service.runIfLockAcquired(key, () -> { })).isTrue();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            //noinspection ResultOfMethodCallIgnored
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
