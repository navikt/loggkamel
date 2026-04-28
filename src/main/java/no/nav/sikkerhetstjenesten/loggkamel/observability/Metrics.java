package no.nav.sikkerhetstjenesten.loggkamel.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class Metrics {

    private static final String LOGGKAMEL_APP_PREFIX = "loggkamel.";

    private final MeterRegistry meterRegistry;
    public final Counter enrichedLogPublished;
    public final Counter logsPostgresConsumed;

    public final Counter intermediateLogConsumed;
    public final Counter intermediateLogProduced;

    public final Counter logsPostgresInvalid;
    public final Counter logsPostgresDeadletter;
    public final Counter logsFallbackInvalid;

    public final Counter logPostgresInvalid;
    public final Counter logPostgresDeadletter;
    public final Counter logFallbackInvalid;

    public Metrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.enrichedLogPublished = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.enriched.published", "enriched", "published");
        this.logsPostgresConsumed = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.postgres.consumed", "postgres", "consumed");

        this.intermediateLogConsumed = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.intermediate.consumed", "intermediate", "consumed");
        this.intermediateLogProduced = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.intermediate.produced", "intermediate", "produced");

        this.logsPostgresInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.postgres.invalid", "postgres", "backout", "invalid");
        this.logsPostgresDeadletter = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.postgres.deadletter", "postgres", "backout", "deadletter");
        this.logsFallbackInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.fallback.invalid", "fallback", "backout", "invalid");

        this.logPostgresInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.postgres.invalid", "postgres", "backout", "invalid");
        this.logPostgresDeadletter = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.postgres.deadletter", "postgres", "backout", "deadletter");
        this.logFallbackInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.fallback.invalid", "fallback", "backout", "invalid");
    }

}
