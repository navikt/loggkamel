package no.nav.sikkerhetstjenesten.loggkamel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.processor.InvalidIndividualPostgresLog;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.PostgresLogEnricher.POSTGRES_LOG_ENRICH_ROUTE;

@Component
public class PostgresLogConsumer extends RouteBuilder {

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.dead-letter}")
    private String deadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    private String invalidMessageUri;

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Override
    public void configure() {
        //TODO: try to set up such that dead letter only handles postgres messages, not all undelivered messages
        // Alternatively, pull out of here and into a universal route definition

        // Delivery / technical failures for otherwise valid messages
        errorHandler(deadLetterChannel(deadLetterUri)
                .maximumRedeliveries(1)
                .useExponentialBackOff()
        );

        //TODO: should narrow exception class to keep this specific to postgres?
        // Invalid logical message format
        onException(InvalidIndividualPostgresLog.class)
                .handled(true)
                .useOriginalMessage()
                .log("Routing invalid message to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}") //TODO: add invalid file name here
                //TODO: encode/marshall as .gz file here
                .to(invalidMessageUri);

        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")

        from(consumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .doTry()
                    .unmarshal().gzipDeflater()
                    //TODO: update filename to remove the .gz at the end here
                .endDoTry()
                .doCatch(IOException.class)
                    .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                    .to(invalidMessageUri)
                    .stop()
                .end()
                .split(body().tokenize("^\\<|\n\\<")).streaming()
                //TODO: update filename to differentiate from all other split entries here
                .to(POSTGRES_LOG_ENRICH_ROUTE);
    }
}
