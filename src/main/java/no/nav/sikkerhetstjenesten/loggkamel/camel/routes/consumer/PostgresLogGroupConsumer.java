package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PostgresLogGroupConsumer.class);
    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";
    private static final String BODY_HASH_PRE_DECOMPRESS = "BODY_HASH_PRE_DECOMPRESS";
    private static final String BODY_SIZE_PRE_DECOMPRESS = "BODY_SIZE_PRE_DECOMPRESS";
    private static final String BODY_TYPE_PRE_DECOMPRESS = "BODY_TYPE_PRE_DECOMPRESS";
    private static final String BODY_HASH_POST_DECOMPRESS = "BODY_HASH_POST_DECOMPRESS";
    private static final String BODY_SIZE_POST_DECOMPRESS = "BODY_SIZE_POST_DECOMPRESS";
    private static final String BODY_TYPE_POST_DECOMPRESS = "BODY_TYPE_POST_DECOMPRESS";

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
//            .convertBodyTo(byte[].class)
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
                .end()
            .to(LOG_GROUP_ENRICHER_ROUTE);
    }
}
