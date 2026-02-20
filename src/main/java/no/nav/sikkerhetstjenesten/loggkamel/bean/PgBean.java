package no.nav.sikkerhetstjenesten.loggkamel.bean;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PgBean {
    private static final Logger log = LoggerFactory.getLogger(PgBean.class);

    public void extract(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);
        //log.error("Message body: " + body);
        exchange.setVariable("test variable", UUID.randomUUID().toString());
        log.error("Exchange variables at the end of bean execution: " + exchange.getVariables());
    }
}