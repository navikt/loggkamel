package no.nav.sikkerhetstjenesten.loggkamel.route;

import no.nav.sikkerhetstjenesten.loggkamel.bean.InvalidAuditMessageException;
import no.nav.sikkerhetstjenesten.loggkamel.bean.PostgresBean;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *
 */
@Component
public class PostgresRoute extends RouteBuilder {

    @Autowired
    private String postgresDeadLetterUri;

    @Autowired
    private String postgresInvalidMessagesUri;

    @Autowired
    private String postgresEntranceUri;

    @Autowired
    private String postgresExitUri;


    @Override
    public void configure() {

        // Delivery / technical failures for otherwise valid messages
        errorHandler(deadLetterChannel(postgresDeadLetterUri)
                .maximumRedeliveries(1)
                .useExponentialBackOff()
        );

        // Invalid logical message format
        onException(InvalidAuditMessageException.class)
                .handled(true)
                .useOriginalMessage()
                .log("Routing invalid message to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}") //TODO: add invalid file name here
                .to(postgresInvalidMessagesUri);

        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")
        //from("timer://myTimer?period=600000")
        from(postgresEntranceUri)
                .routeId("pg-file-ingest")
                .doTry()
                    .unmarshal().gzipDeflater()
                    .endDoTry()
                .doCatch(IOException.class)
                    .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                    .to(postgresInvalidMessagesUri)
                    .stop()
                .end()
                .split(simple("${body}").tokenize("^\\<|\n\\<"))
                .log(LoggingLevel.INFO, "Message: ${body}, Headers: ${headers}")
                .bean(PostgresBean.class, "extract")
                //TODO: remove or update logging level for output logging
                .log(LoggingLevel.INFO, "Per-message variables visible in the route after bean execution: ${variables}")
                // TODO: broader: how to manually set the file name here?
                // done by setting header, but can you get one message to result in a file for every line? What would you name each line?
                .toD(postgresExitUri);

    }
}
