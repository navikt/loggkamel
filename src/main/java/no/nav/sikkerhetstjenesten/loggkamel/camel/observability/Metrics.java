package no.nav.sikkerhetstjenesten.loggkamel.camel.observability;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import org.springframework.stereotype.Component;

@Component
public class Metrics {

    private static final String LOGGKAMEL_APP_PREFIX = "loggkamel.auditlogs.";
    private static final String HAPPY_PATH_METRIC = LOGGKAMEL_APP_PREFIX + "happy";
    private static final String BACKOUT_QUEUE_METRIC = LOGGKAMEL_APP_PREFIX + "backout";
    private static final String UNIQUE_DATABASE_ACTION_METRIC = LOGGKAMEL_APP_PREFIX + "unik";
    private static final String UNKNOWN_NAV_IDENT_METRIC = LOGGKAMEL_APP_PREFIX + "unknown";
    private static final String DB2_STATEMENT_ISSUE_TYPE_METRIC = LOGGKAMEL_APP_PREFIX + "statement.issue";

    private static final String MULTIPLICITY_LABEL = "multiplicity";
    private static final String TEKNOLOGI_LABEL = "teknologi";
    private static final String ACTION_LABEL = "action";
    private static final String DATABASE_LABEL = "database";
    private static final String DB2_ISSUE_TYPE_LABEL = "type";

    public enum Action {produced, consumed}

    public enum Multiplicity {stream, packet, line}

    public enum DB2IssueType {unparsable, unexpectedStatementType}

    private final MeterRegistry meterRegistry;

    public Metrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        //initialize counters with bounded possible values
        for (Multiplicity multiplicity : Multiplicity.values()) {
            for (TeknologiEnum teknologi : TeknologiEnum.values()) {
                for (Action action : Action.values()) {
                    meterRegistry.counter(HAPPY_PATH_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), ACTION_LABEL, action.name());
                }

                meterRegistry.counter(BACKOUT_QUEUE_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase());
            }
        }

        meterRegistry.counter(UNKNOWN_NAV_IDENT_METRIC);

        for (DB2IssueType db2IssueType : DB2IssueType.values()) {
            meterRegistry.counter(DB2_STATEMENT_ISSUE_TYPE_METRIC, DB2_ISSUE_TYPE_LABEL, db2IssueType.name());
        }
    }

    public void incrementHappyPath(Multiplicity multiplicity, TeknologiEnum teknologi, Action action) {
        meterRegistry.counter(HAPPY_PATH_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), ACTION_LABEL, action.name()).increment();
    }

    public void incrementBackoutQueueMetrics(Multiplicity multiplicity, TeknologiEnum teknologi) {
        meterRegistry.counter(BACKOUT_QUEUE_METRIC, MULTIPLICITY_LABEL, multiplicity.name(), TEKNOLOGI_LABEL, teknologi.name().toLowerCase()).increment();
    }

    public void incrementDatabaseSpecificAction(String databaseName, TeknologiEnum teknologi, Action action) {
        meterRegistry.counter(UNIQUE_DATABASE_ACTION_METRIC, TEKNOLOGI_LABEL, teknologi.name().toLowerCase(), ACTION_LABEL, action.name(), DATABASE_LABEL, databaseName).increment();
    }

    public void incrementUnknownNavIdent() {
        meterRegistry.counter(UNKNOWN_NAV_IDENT_METRIC).increment();
    }

    public void incrementDB2Issue(DB2IssueType db2IssueType) {
        meterRegistry.counter(DB2_STATEMENT_ISSUE_TYPE_METRIC, DB2_ISSUE_TYPE_LABEL, db2IssueType.name()).increment();
    }

}
