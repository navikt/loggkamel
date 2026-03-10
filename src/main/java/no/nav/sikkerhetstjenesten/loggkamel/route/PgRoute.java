package no.nav.sikkerhetstjenesten.loggkamel.route;

import no.nav.sikkerhetstjenesten.loggkamel.bean.InvalidAuditMessageException;
import no.nav.sikkerhetstjenesten.loggkamel.bean.PgBean;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *
 */
@Component
public class PgRoute extends RouteBuilder {

    private static final String DEAD_LETTER_URI =
            "file:src/main/resources/files/output/dead-letter?fileExist=Append";

    private static final String INVALID_MESSAGES_URI =
            "file:src/main/resources/files/output/invalid-messages?fileExist=Append";


    @Override
    public void configure() {

        // Delivery / technical failures for otherwise valid messages
        errorHandler(deadLetterChannel(DEAD_LETTER_URI)
                .maximumRedeliveries(1)
                .useExponentialBackOff()
        );

        // Invalid logical message format
        onException(InvalidAuditMessageException.class)
                .handled(true)
                .useOriginalMessage()
                .log("Routing invalid message to invalid-messages channel: ${exception.message}")
                .to(INVALID_MESSAGES_URI);

        // TODO: use injected Endpoints to define the in and out points for the messages
        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")
        //from("timer://myTimer?period=600000")
        from("file:src/main/resources/files?noop=true")
                .routeId("pg-file-ingest")
                .doTry()
                    .unmarshal().gzipDeflater()
                    .endDoTry()
                .doCatch(IOException.class)
                    .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}")
                    .to(INVALID_MESSAGES_URI)
                    .stop()
                .end()
                .split(simple("${body}").tokenize("^\\<|\n\\<"))
                .log("Message: ${body}, Headers: ${headers}")
                .bean(PgBean.class, "extract")
                .log("Per-message variables visible in the route after bean execution: ${variables}")
                // TODO: Have the output location depend on (incorporate) db name
                .toD("file:src/main/resources/files/output/?fileExist=Append");

    }
}
