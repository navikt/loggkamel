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

            exchange.setVariable("logTime", matcher.group(1));
            exchange.setVariable("navIdent", matcher.group(2));
            exchange.setVariable("dbName", matcher.group(3));
            exchange.setVariable("logType", matcher.group(4));
            exchange.setVariable("sqlCommand", matcher.group(5));

            msg.setBody(body);
        } else {
            log.error("Log failed to match expected pattern");
//            throw new RuntimeException("Log failed to match expected pattern");
        }

    }
}