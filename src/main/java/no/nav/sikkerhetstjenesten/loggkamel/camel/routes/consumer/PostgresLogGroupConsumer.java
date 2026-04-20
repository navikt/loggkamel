package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogGroupEnricher.LOG_GROUP_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class PostgresLogGroupConsumer extends SharedRouteErrorHandler {

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
            .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}")
            .log(LoggingLevel.INFO, "Consuming postgres log messages from ${header.CamelFileName}")
            .process(exchange -> {
                exchange.setVariable(TEKNOLOGI, TeknologiEnum.POSTGRESQL);
            })
            .log(LoggingLevel.INFO, "Conditionally decompressing log message ${header.CamelFileName}")
            .choice()
                .when(header(FILE_NAME).endsWith(".gz"))
                    .log(LoggingLevel.INFO, "Log file ${header.CamelFileName} is gzip compressed, attempting to decompress")
                    // if log file is compressed, decompress and remove the compression extension from the filename
                    .doTry()
                        .unmarshal().gzipDeflater()
                        .endDoTry()
                    .doCatch(Exception.class)
                        .throwException(new InvalidPostgresLogGroupException("Failed to decompress gzip file ${header.CamelFileName}, error: ${exception.message}"))
                    .end()
                    .process(exchange -> {
                        String originalFileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                        String newFileName = originalFileName.substring(0, originalFileName.length() - 3);

                        exchange.getIn().setHeader(FILE_NAME, newFileName);
                    })
                .end()
            .to(LOG_GROUP_ENRICHER_ROUTE);
    }
}
