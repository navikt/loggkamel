package no.nav.sikkerhetstjenesten.loggkamel.camel.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LazyCounterInitializerTest {

    private static final String METRIC_NAME = "test.counter";
    private static final String LABEL_NAME = "database";
    private static final String LABEL_VALUE = "database";

    @Spy
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Mock
    TaskScheduler taskScheduler;

    @InjectMocks
    LazyCounterInitializer lazyCounterInitializer;

    @Test
    void increment_registersAtZeroAndFlushesPendingIncrements() {
        lazyCounterInitializer.increment(METRIC_NAME, LABEL_NAME, LABEL_VALUE);

        Counter counter = counter();
        assertEquals(0, counter.count());

        lazyCounterInitializer.increment(METRIC_NAME, LABEL_NAME, LABEL_VALUE);
        assertEquals(0, counter.count());

        scheduledFlush().run();
        assertEquals(2, counter.count());

        lazyCounterInitializer.increment(METRIC_NAME, LABEL_NAME, LABEL_VALUE);
        assertEquals(3, counter.count());
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void increment_doesNotLoseConcurrentIncrements() {
        int incrementCount = 1_000;

        IntStream.range(0, incrementCount)
                .parallel()
                .forEach(ignored -> lazyCounterInitializer.increment(METRIC_NAME, LABEL_NAME, LABEL_VALUE));

        scheduledFlush().run();

        assertEquals(incrementCount, counter().count());
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void increment_flushesImmediatelyWhenSchedulingIsRejected() {
        doThrow(new TaskRejectedException("rejected"))
                .when(taskScheduler)
                .schedule(any(Runnable.class), any(Instant.class));

        lazyCounterInitializer.increment(METRIC_NAME, LABEL_NAME, LABEL_VALUE);

        assertEquals(1, counter().count());
    }

    private Counter counter() {
        return meterRegistry.get(METRIC_NAME)
                .tag(LABEL_NAME, LABEL_VALUE)
                .counter();
    }

    private Runnable scheduledFlush() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        return runnableCaptor.getValue();
    }
}
