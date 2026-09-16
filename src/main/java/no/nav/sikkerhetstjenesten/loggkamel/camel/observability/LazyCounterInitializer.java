package no.nav.sikkerhetstjenesten.loggkamel.camel.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LazyCounterInitializer {

    private static final Logger log = LoggerFactory.getLogger(LazyCounterInitializer.class);

    private static final Duration COUNTER_INITIALIZATION_DELAY = Duration.ofSeconds(61);
    private static final long CLOSED_PENDING_COUNTER = -1;

    private final MeterRegistry meterRegistry;
    private final TaskScheduler taskScheduler;
    private final Map<CounterKey, PendingCounter> counterStates = new ConcurrentHashMap<>();

    public LazyCounterInitializer(MeterRegistry meterRegistry, TaskScheduler taskScheduler) {
        this.meterRegistry = meterRegistry;
        this.taskScheduler = taskScheduler;
    }

    public void increment(String metricName, String... tags) {
        CounterKey key = new CounterKey(metricName, List.copyOf(Arrays.asList(tags)));
        PendingCounter pendingCounter = counterStates.get(key);

        // If counter has not been incremented in this deployment's lifetime
        if (pendingCounter == null) {
            Counter counter = meterRegistry.counter(metricName, tags);
            PendingCounter newPendingCounter = new PendingCounter(counter);
            PendingCounter existingPendingCounter = counterStates.putIfAbsent(key, newPendingCounter);

            // If there were multiple attempts to increment from separate threads, only the first thread to hit this schedules a flush
            if (existingPendingCounter == null) {
                scheduleFlush(newPendingCounter);
                incrementPendingOrCounter(newPendingCounter);
                return;
            }

            pendingCounter = existingPendingCounter;
        }

        incrementPendingOrCounter(pendingCounter);
    }

    private void scheduleFlush(PendingCounter pendingCounter) {
        try {
            taskScheduler.schedule(
                    () -> flushPendingCounter(pendingCounter),
                    Instant.now().plus(COUNTER_INITIALIZATION_DELAY)
            );
        } catch (TaskRejectedException e) {
            log.warn("Could not schedule lazy counter initialization; flushing the counter immediately", e);
            flushPendingCounter(pendingCounter);
        }
    }

    private void incrementPendingOrCounter(PendingCounter pendingCounter) {
        // Try to add an increment to those scheduled for this counter. If that fails, the pending increments have
        // been executed and we can increment the counter directly (the first scrape has happened and this increment cannot be lost)
        if (!pendingCounter.tryIncrement()) {
            pendingCounter.counter().increment();
        }
    }

    private void flushPendingCounter(PendingCounter pendingCounter) {
        long increments = pendingCounter.close();
        if (increments > 0) {
            pendingCounter.counter().increment(increments);
        }
    }

    private record CounterKey(String metricName, List<String> tags) {}

    private record PendingCounter(Counter counter, AtomicLong increments) {

        private PendingCounter(Counter counter) {
            this(counter, new AtomicLong());
        }

        private boolean tryIncrement() {
            long current;
            do {
                current = increments.get();
                if (current == CLOSED_PENDING_COUNTER) {
                    return false;
                }
            } while (!increments.compareAndSet(current, current + 1));
            return true;
        }

        private long close() {
            return increments.getAndSet(CLOSED_PENDING_COUNTER);
        }
    }
}
