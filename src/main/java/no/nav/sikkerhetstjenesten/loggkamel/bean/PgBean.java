package no.nav.sikkerhetstjenesten.loggkamel.bean;

import no.nav.sikkerhetstjenesten.loggkamel.config.EntraProxyAnsatt;
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
public class PgBean {
    private static final Logger log = LoggerFactory.getLogger(PgBean.class);

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
    public PgBean(EntraProxyService entraProxyService) {
        this.entraProxyService = entraProxyService;
    }

    public void extract(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        // Check if the input string matches the template pattern
        if (!matcher.find()) {
            log.error("Log failed to match expected pattern");
            // TODO: more apt/targeted exception here?
            throw new RuntimeException("Log failed to match expected pattern, log: " + body);
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
            log.error("Error when fetching employee info", e);
            throw e;
        }
        String navEpost = entraProxyAnsatt.getEPost();
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
        exchange.setVariable(NAV_EPOST, navEpost);

        msg.setBody(body);
    }
}