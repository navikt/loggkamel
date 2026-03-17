package no.nav.sikkerhetstjenesten.loggkamel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.PostgresLogEnricher.POSTGRES_LOG_ENRICH_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class PostgresLogConsumer extends SharedRouteErrorHandler {

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Override
    public void configure() {
        super.errorHandling();

        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")

        from(consumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .process(exchange -> {
                    // If the file comes from a bucket instead of local storage, still populate the filename
                    if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
                        exchange.getIn().setHeader(FILE_NAME, exchange.getIn().getHeader("CamelGoogleCloudStorageObjectName", String.class));
                    }
                })
                .log(LoggingLevel.INFO, "Processing postgres log message from ${header.CamelFileName}")
                //TODO: split out DB name, put into header variables here
                .choice()
                .when(header(FILE_NAME).endsWith(".gz"))
                    .doTry()
                        .unmarshal().gzipDeflater()
                    .endDoTry()
                    .doCatch(IOException.class)
                        .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                        .to(invalidMessageUri)
                        .stop()
                    .end()
                .end()
                // Split the log file by lines, and strip the leading "<" symbols
                .split(body().tokenize("^\\<|\n\\<")).streaming()
                .process(exchange -> {
                    String originalFileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                    // If a line was part of a compressed file, strip compression extension and make unique with a UUID
                    if (originalFileName.endsWith(".gz")) {
                        String newFileName = UUID.randomUUID() + "." + originalFileName.substring(0, originalFileName.length() - 3);
                        exchange.getIn().setHeader(FILE_NAME, newFileName);
                    }
                })
                .to(POSTGRES_LOG_ENRICH_ROUTE);
    }
}
