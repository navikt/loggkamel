package no.nav.sikkerhetstjenesten.loggkamel.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class Metrics {

    private static final String LOGGKAMEL_APP_PREFIX = "loggkamel.auditlogs.";

    public enum Action {produced, consumed}

    public enum Multiplicity {grouped, single}

    public enum BackoutQueueType {invalid, deadletter};

    private final MeterRegistry meterRegistry;

    public Metrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementHappyPath(Multiplicity multiplicity, String teknologi, Action action) {
        String logName = LOGGKAMEL_APP_PREFIX + "happy";
        meterRegistry.counter(logName, "multiplicity", multiplicity.name(), "teknologi", teknologi, "action", action.name()).increment();
    }

    public void incrementUnhappyPath(Multiplicity multiplicity, String teknologi, BackoutQueueType backoutQueueType) {
        String logName = LOGGKAMEL_APP_PREFIX + "backout";
        meterRegistry.counter(logName, "multiplicity", multiplicity.name(), "teknologi", teknologi, "queue", backoutQueueType.name()).increment();
    }

    public void incrementDatabaseSpecificAction(String databaseName, String teknologi, Action action) {
        String logName = LOGGKAMEL_APP_PREFIX + "unik";
        meterRegistry.counter(logName, "teknologi", teknologi, "action", action.name(), "database", databaseName).increment();
    }

    public void incrementUnknownNavIdent() {
        String logName = LOGGKAMEL_APP_PREFIX + "unknown-nav-ident";
        meterRegistry.counter(logName).increment();
    }

}
