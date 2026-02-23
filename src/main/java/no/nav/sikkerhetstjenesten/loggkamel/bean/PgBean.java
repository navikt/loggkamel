package no.nav.sikkerhetstjenesten.loggkamel.bean;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public void extract(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(READ|WRITE|FUNCTION|DDL|MISC|MISC_SET),.*?,.*?,.*?,\"([\\s\\S]*)\",(.*)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        // Check if the input string matches the template pattern
        if (matcher.find()) {
//            log.info("time: " + matcher.group(1));
//            log.info("navIdent: " + matcher.group(2));
//            log.info("dbName: " + matcher.group(3));
//            log.info("pgAudit Log Type: " + matcher.group(4));
//            log.info("SQL command: " + matcher.group(5));
//            log.info("rest: " + matcher.group(6));

            exchange.setVariable(LOG_TIME, matcher.group(1));
            exchange.setVariable(NAV_IDENT, matcher.group(2));
            exchange.setVariable(DB_NAME, matcher.group(3));
            exchange.setVariable(LOG_TYPE, matcher.group(4));
            exchange.setVariable(SQL_COMMAND, matcher.group(5));

            msg.setBody(body);
        } else {
            log.error("Log failed to match expected pattern");
            throw new RuntimeException("Log failed to match expected pattern");
        }

    }
}