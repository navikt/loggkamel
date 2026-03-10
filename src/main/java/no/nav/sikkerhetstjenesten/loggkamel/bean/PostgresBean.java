package no.nav.sikkerhetstjenesten.loggkamel.bean;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
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
public class PostgresBean {
    private static final Logger log = LoggerFactory.getLogger(PostgresBean.class);

    static final String UNEXPECTED_LOG_PATTERN_MESSAGE = "Log failed to match expected pattern";
    static final String ENTRA_PROXY_ERROR_MESSAGE = "Error when fetching employee info";

    static final String LOG_TIME = "logTime";
    static final String NAV_IDENT = "navIdent";
    static final String DB_NAME = "dbName";
    static final String AUDIT_TYPE = "auditType";
    static final String STATEMENT_ID = "statementId";
    static final String SUBSTATEMENT_ID = "substatementId";
    static final String PG_AUDIT_CLASS = "auditClass";
    static final String PG_AUDIT_COMMAND = "auditCommand";
    static final String PG_AUDIT_OBJECT_TYPE = "auditType";
    static final String PG_AUDIT_OBJECT_NAME = "auditName";
    static final String SQL_STATEMENT = "sqlStatement";
    static final String SQL_PARAMETER = "sqlParameter";
    static final String NAV_EPOST = "navEpost";

    private final EntraProxyService entraProxyService;

    @Autowired
    public PostgresBean(EntraProxyService entraProxyService) {
        this.entraProxyService = entraProxyService;
    }

    public void extract(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        if (body == null || body.isBlank()) {
            throw new InvalidAuditMessageException("Audit log message is blank");
        }

        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            log.warn(UNEXPECTED_LOG_PATTERN_MESSAGE);
            throw new InvalidAuditMessageException(UNEXPECTED_LOG_PATTERN_MESSAGE);
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
            throw new InvalidAuditMessageException(ENTRA_PROXY_ERROR_MESSAGE, e);
        }
        exchange.setVariable(LOG_TIME, logTime);
        exchange.setVariable(NAV_IDENT, navIdent);
        exchange.setVariable(DB_NAME, dbName);
        exchange.setVariable(AUDIT_TYPE, auditType);
        exchange.setVariable(STATEMENT_ID, statementId);
        exchange.setVariable(SUBSTATEMENT_ID, substatementId);
        exchange.setVariable(PG_AUDIT_CLASS, pgAuditClass);
        exchange.setVariable(PG_AUDIT_COMMAND, pgAuditCommand);
        exchange.setVariable(PG_AUDIT_OBJECT_TYPE, pgAuditObjectType);
        exchange.setVariable(PG_AUDIT_OBJECT_NAME, pgAuditObjectName);
        exchange.setVariable(SQL_STATEMENT, sqlStatement);
        exchange.setVariable(SQL_PARAMETER, sqlParameter);
        String navEpost = entraProxyAnsatt.getEPost();
        exchange.setVariable(NAV_EPOST, navEpost);

        msg.setBody(body);
    }
}