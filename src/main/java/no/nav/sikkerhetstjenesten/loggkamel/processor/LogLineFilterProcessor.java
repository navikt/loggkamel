package no.nav.sikkerhetstjenesten.loggkamel.processor;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogLineFilterProcessor.class);

    public boolean doSomething(Exchange exchange) {
        log.info("LogFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME));

        //TODO: pull the database being operated on from the exchange header variables

        //TODO: query local DB to get logging flags for associated DB

        //TODO: build set of logging actions we care about for the given set of logging flags

        //TODO: determine whether to continue logging or drop the message based on its logging action and set of actions of interest

        return true;
    }
}
