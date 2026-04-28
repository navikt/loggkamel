package no.nav.sikkerhetstjenesten.loggkamel.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class Metrics {

    private static final String LOGGKAMEL_APP_PREFIX = "loggkamel.";

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
        this.enrichedLogPublished = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.enriched.published", "type", "enriched", "action", "published");
        this.logsPostgresConsumed = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.postgres.consumed", "type", "postgres", "action", "consumed");

        this.intermediateLogConsumed = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.intermediate.consumed", "type", "intermediate", "action", "consumed");
        this.intermediateLogProduced = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.intermediate.produced", "type", "intermediate", "action", "produced");

        this.logsPostgresInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.postgres.invalid", "type", "postgres", "queue", "invalid");
        this.logsPostgresDeadletter = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.postgres.deadletter", "type", "postgres", "queue", "deadletter");
        this.logsFallbackInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "logs.fallback.invalid", "type", "fallback", "queue", "invalid");

        this.logPostgresInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.postgres.invalid", "type", "postgres", "queue", "invalid");
        this.logPostgresDeadletter = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.postgres.deadletter", "type", "postgres", "queue", "deadletter");
        this.logFallbackInvalid = meterRegistry.counter(LOGGKAMEL_APP_PREFIX + "log.fallback.invalid", "type", "fallback", "queue", "invalid");
    }

}
