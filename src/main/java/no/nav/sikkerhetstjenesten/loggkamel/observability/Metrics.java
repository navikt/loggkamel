package no.nav.sikkerhetstjenesten.loggkamel.observability;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.springframework.stereotype.Component;

@Component
public class Metrics {

    private static final String LOGGKAMEL_APP_PREFIX = "loggkamel.auditlogs.";
    private static final String HAPPY_PATH_METRIC = LOGGKAMEL_APP_PREFIX + "happy";
    private static final String UNHAPPY_PATH_METRIC = LOGGKAMEL_APP_PREFIX + "backout";
    private static final String UNIQUE_DATABASE_ACTION_METRIC = LOGGKAMEL_APP_PREFIX + "unik";
    private static final String UNKNOWN_NAV_IDENT_METRIC = LOGGKAMEL_APP_PREFIX + "unknown";

    private static final String MULTIPLICITY_LABEL = "multiplicity";
    private static final String TEKNOLOGI_LABEL = "teknologi";
    private static final String ACTION_LABEL = "action";
    private static final String QUEUE_LABEL  = "queue";
    private static final String DATABASE_LABEL = "database";

    public enum Action {produced, consumed}

    public enum Multiplicity {grouped, single}

    public enum BackoutQueueType {invalid, deadletter};

    private final MeterRegistry meterRegistry;

    public Metrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        //initialize counters with bounded possible values
        for (Multiplicity multiplicity : Multiplicity.values()) {
            for (TeknologiEnum teknologi : TeknologiEnum.values()) {
                for (Action action : Action.values()) {
                    meterRegistry.counter(HAPPY_PATH_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), ACTION_LABEL, action.name());
                }
                for (BackoutQueueType backoutQueueType : BackoutQueueType.values()) {
                    meterRegistry.counter(UNHAPPY_PATH_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), QUEUE_LABEL, backoutQueueType.name());
                }
            }
        }
        meterRegistry.counter(UNKNOWN_NAV_IDENT_METRIC);
    }

    public void incrementHappyPath(Multiplicity multiplicity, TeknologiEnum teknologi, Action action) {
        meterRegistry.counter(HAPPY_PATH_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), ACTION_LABEL, action.name());
    }

    public void incrementUnhappyPath(Multiplicity multiplicity, TeknologiEnum teknologi, BackoutQueueType backoutQueueType) {
        meterRegistry.counter(UNHAPPY_PATH_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), QUEUE_LABEL, backoutQueueType.name()).increment();
    }

    public void incrementDatabaseSpecificAction(String databaseName, TeknologiEnum teknologi, Action action) {
        meterRegistry.counter(UNIQUE_DATABASE_ACTION_METRIC, TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), ACTION_LABEL, action.name(), DATABASE_LABEL, databaseName).increment();
    }

    public void incrementUnknownNavIdent() {
        meterRegistry.counter(UNKNOWN_NAV_IDENT_METRIC).increment();
    }

}
