package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogGroupConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogGroupEnricher.LOG_GROUP_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class PostgresLogGroupConsumer extends LoggGroupErrorHandler {

    private static final String KEEP_SOURCE_FILE = "keepSourceFile";

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Value("${routing.postgres.consumer-delete:#{null}}")
    private String deleteSourceUri;

    @Autowired
    @Qualifier("postgresLogGroupIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    @Override
    public void configure() {
        // Explicitly delete original local files on route completion. Only necessary when reading to/from GCP
        if (deleteSourceUri != null && deleteSourceUri.startsWith("google-storage://")) {
            onCompletion()
                    .onWhen(simple("${exchangeProperty." + KEEP_SOURCE_FILE + "} != true && ${header.CamelDuplicateMessage} != true"))
                    .setHeader(OBJECT_NAME, header(FILE_NAME))
                    .log(LoggingLevel.INFO, "Deleting consumed source object ${header.CamelFileName} from consumer bucket")
                    .to(deleteSourceUri);
        }

        this.errorHandling();

        onException(DuplicateKeyException.class)
                .log("Caught DuplicateKeyException when trying to claim filename: ${headers['CamelFileName']}, aborting processing without removing source file")
                .setProperty(KEEP_SOURCE_FILE, constant(true))
                .handled(true);

        from(consumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .autoStartup(false)
                .transacted()
                .bean(PostgresLogGroupConsumerProcessor.class, "initializeConsumerState")
                .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName}")
                //Prevent multiple instances of loggkamel from processing the same file, leave removal of the file up to the instance processing it
                .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true).removeOnFailure(false)
                .log(LoggingLevel.INFO, "Consuming postgres log messages as filename: ${header.CamelFileName}")
                .convertBodyTo(byte[].class) // Ensure body is fully read and cached for use in error handling, as with GCP buckets the body is an InputStream that can only be read once
                .bean(PostgresLogGroupConsumerProcessor.class, "incrementMetrics")
                .bean(PostgresLogGroupConsumerProcessor.class, "decompressIfGzip")
                .to(LOG_GROUP_ENRICHER_ROUTE);
    }
}
