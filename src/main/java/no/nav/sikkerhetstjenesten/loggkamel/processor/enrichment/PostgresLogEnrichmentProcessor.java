package no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.processor.InvalidIndividualPostgresLog;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostgresLogEnrichmentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostgresLogEnrichmentProcessor.class);

    static final String UNEXPECTED_LOG_PATTERN_MESSAGE = "Log failed to match expected pattern";
    static final String ENTRA_PROXY_ERROR_MESSAGE = "Error when fetching employee info";

//    static final String LOG_TIME = "logTime";
//    static final String NAV_IDENT = "navIdent";
//    static final String DB_NAME = "dbName";
//    static final String AUDIT_TYPE = "auditType";
//    static final String STATEMENT_ID = "statementId";
//    static final String SUBSTATEMENT_ID = "substatementId";
//    static final String PG_AUDIT_CLASS = "auditClass";
//    static final String PG_AUDIT_COMMAND = "auditCommand";
//    static final String PG_AUDIT_OBJECT_TYPE = "pgAuditType";
//    static final String PG_AUDIT_OBJECT_NAME = "pgAuditName";
//    static final String SQL_STATEMENT = "sqlStatement";
//    static final String SQL_PARAMETER = "sqlParameter";
//    static final String NAV_EPOST = "navEpost";

    public static final String LOG_ENRICHMENT = "logEnrichment";

    private final EntraProxyService entraProxyService;

    @Autowired
    public PostgresLogEnrichmentProcessor(EntraProxyService entraProxyService) {
        this.entraProxyService = entraProxyService;
    }

    public void enrich(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        if (body == null || body.isBlank()) {
            throw new InvalidIndividualPostgresLog("Audit log message is blank");
        }

        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            //TODO: add identifier for the log line that doesn't leak PII here
            log.warn(UNEXPECTED_LOG_PATTERN_MESSAGE);
            throw new InvalidIndividualPostgresLog(UNEXPECTED_LOG_PATTERN_MESSAGE);
        }

        String logTime = matcher.group(1);
        String navIdent = matcher.group(2);
        String dbName = matcher.group(3);
        String auditType = matcher.group(4);
        String statementId = matcher.group(5);
        String substatementId = matcher.group(6);
        String pgAuditClass = matcher.group(7);
        String pgAuditCommand = matcher.group(8);
        String pgAuditObjectType = matcher.group(9);
        String pgAuditObjectName = matcher.group(10);
        String sqlStatement = matcher.group(12);
        String sqlParameter = matcher.group(14);

        EntraProxyAnsatt entraProxyAnsatt;
        try {
            entraProxyAnsatt = entraProxyService.getAnsattFromNavIdent(navIdent);

            // TODO: handle null or empty response here
        } catch (Exception e) {
            // TODO: handle exceptions resulting from entra-proxy errors or service unavailable
            log.error(ENTRA_PROXY_ERROR_MESSAGE, e);
            throw new InvalidIndividualPostgresLog(ENTRA_PROXY_ERROR_MESSAGE, e);
        }

        //TODO: convert this from an unstructured map to a postgresql-specific object
//        Map<String, Object> logValues = new HashMap<>();
//
//        logValues.put(LOG_TIME, logTime);
//        logValues.put(NAV_IDENT, navIdent);
//        logValues.put(DB_NAME, dbName);
//        logValues.put(AUDIT_TYPE, auditType);
//        logValues.put(STATEMENT_ID, statementId);
//        logValues.put(SUBSTATEMENT_ID, substatementId);
//        logValues.put(PG_AUDIT_CLASS, pgAuditClass);
//        logValues.put(PG_AUDIT_COMMAND, pgAuditCommand);
//        logValues.put(PG_AUDIT_OBJECT_TYPE, pgAuditObjectType);
//        logValues.put(PG_AUDIT_OBJECT_NAME, pgAuditObjectName);
//        logValues.put(SQL_STATEMENT, sqlStatement);
//        logValues.put(SQL_PARAMETER, sqlParameter);
//        logValues.put(NAV_EPOST, entraProxyAnsatt.getEpost());

        PostgresLogEnrichment logEnrichment = PostgresLogEnrichment.builder()
                .logTime(logTime)
                .navIdent(navIdent)
                .dbName(dbName)
                .auditType(auditType)
                .statementId(statementId)
                .substatementId(substatementId)
                .pgAuditClass(pgAuditClass)
                .pgAuditCommand(pgAuditCommand)
                .pgAuditObjectType(pgAuditObjectType)
                .pgAuditObjectName(pgAuditObjectName)
                .sqlStatement(sqlStatement)
                .sqlParameter(sqlParameter)
                .epost(entraProxyAnsatt.getEpost())
                .build();

        exchange.setVariable(LOG_ENRICHMENT, logEnrichment);

        msg.setBody(body);
    }
}