package no.nav.sikkerhetstjenesten.loggkamel.bean;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Service;

@Service
public class PgBean {
    public void extract(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);
    }
}