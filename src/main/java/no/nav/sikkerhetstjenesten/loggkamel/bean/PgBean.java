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
    static final String LOG_TYPE = "logType";
    static final String SQL_COMMAND = "sqlCommand";
    static final String SQL_PARAMS = "sqlParams";
    static final String NAV_EPOST = "navEpost";

    private final EntraProxyService entraProxyService;

    @Autowired
    public PgBean(EntraProxyService entraProxyService) {
        this.entraProxyService = entraProxyService;
    }

    public void extract(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(READ|WRITE|FUNCTION|DDL|MISC|MISC_SET),.*?,.*?,.*?,\"([\\s\\S]*)\",(.*)";

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
        String logType = matcher.group(4);
        String sqlCommand = matcher.group(5);
        String sqlParameters = matcher.group(6);

        EntraProxyAnsatt entraProxyAnsatt;
        try {
            entraProxyAnsatt = entraProxyService.getAnsattFromNavIdent(navIdent);
        } catch (Exception e) {
            log.error("Error when fetching employee info", e);
            throw e;
        }
        String navEpost = entraProxyAnsatt.getEPost();

        exchange.setVariable(LOG_TIME, logTime);
        exchange.setVariable(NAV_IDENT, navIdent);
        exchange.setVariable(DB_NAME, dbName);
        exchange.setVariable(LOG_TYPE, logType);
        exchange.setVariable(SQL_COMMAND, sqlCommand);
        exchange.setVariable(SQL_PARAMS, sqlParameters);
        exchange.setVariable(NAV_EPOST, navEpost);

        msg.setBody(body);
    }
}