package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogGroupEnricher.LOG_GROUP_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class PostgresLogGroupConsumer extends LoggGroupErrorHandler {

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Autowired
    @Qualifier("postgresLogGroupIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    @Override
    public void configure() {
        super.errorHandling();

        from(consumerUri)
            .routeId(POSTGRES_LOG_CONSUMER_ID)
            .process(exchange -> exchange.setVariable(TEKNOLOGI, TeknologiEnum.POSTGRESQL))
            .process(exchange -> {
                // If the file comes from a bucket instead of local storage, still populate the filename
                if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
                    exchange.getIn().setHeader(FILE_NAME, exchange.getIn().getHeader(OBJECT_NAME, String.class));
                }
            })
            .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true)
            .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}")
            .log(LoggingLevel.INFO, "Consuming postgres log messages from ${header.CamelFileName}")
            .process(exchange -> metrics.logsPostgresConsumed.increment())
            .choice()
                .when(header(FILE_NAME).endsWith(".gz"))
                    .log(LoggingLevel.INFO, "Log file ${header.CamelFileName} is gzip compressed, attempting to decompress")
                    // if log file is compressed, decompress and remove the compression extension from the filename
                    .doTry()
                        .unmarshal().gzipDeflater()
                        //TODO: confirm this works with repeatedly backed out messages, either remove comment or remove code if does not fix issue
                        .process(exchange -> {
                            // Force body materialization to byte array to ensure the decompressed stream is fully consumed
                            // and captured in-memory, preventing issues with stream references becoming stale in error handling
                            byte[] body = exchange.getIn().getBody(byte[].class);
                            exchange.getIn().setBody(body);
                        })
                        .endDoTry()
                    .doCatch(Exception.class)
                        .process(exchange -> {
                            String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                            Exception cause = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
                            String errorMessage = cause != null ? cause.getMessage() : "unknown error";

                            throw new InvalidPostgresLogGroupException(
                                "Failed to decompress gzip file " + (fileName != null ? fileName : "unknown") + ", error: " + errorMessage
                            );
                        })
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
