package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogStreamConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.NativeLogStreamEnricher.NATIVE_LOG_STREAM_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class PostgresLogStreamConsumer extends LogStreamErrorHandler {

    private static final String KEEP_SOURCE_FILE = "keepSourceFile";

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-stream-consumer";

    @Value("${routing.postgres.consumer}")
    private String postgresStreamConsumerUri;

    @Autowired
    @Qualifier("postgresLogStreamIdempotentRepository")
    private JdbcMessageIdRepository postgresLogStreamIdempotentRepository;

    @Override
    public void configure() {
        // Explicitly delete original local files on route completion. Only necessary when reading from GCP
        if (postgresStreamConsumerUri.startsWith("google-storage://")) {
            onCompletion()
                    .onWhen(simple("${exchangeProperty." + KEEP_SOURCE_FILE + "} != true && ${header.CamelDuplicateMessage} != true"))
                    .setHeader(OBJECT_NAME, header(ORIGINAL_FILENAME))
                    .setHeader(GoogleCloudStorageConstants.OPERATION, () -> GoogleCloudStorageOperations.deleteObject)
                    .setBody(constant(null))
                    .log(LoggingLevel.INFO, "Deleting consumed source object ${header.originalFilename} from consumer bucket")
                    .to(postgresStreamConsumerUri);
        }

        this.errorHandling();

        onException(DuplicateKeyException.class)
                .log(LoggingLevel.INFO, "Caught DuplicateKeyException when trying to claim filename: ${headers['CamelFileName']}, aborting processing without removing source file")
                .setProperty(KEEP_SOURCE_FILE, constant(true))
                .handled(true);

        from(postgresStreamConsumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .streamCache(false)
                .autoStartup(false)
                .transacted()
                .bean(PostgresLogStreamConsumerProcessor.class, "initializeConsumerState")
                .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName}, determining whether to process or if it's already claimed")
                //Prevent multiple instances of loggkamel from processing the same file, leave removal of the file up to the instance processing it
                .idempotentConsumer(header(FILE_NAME), postgresLogStreamIdempotentRepository).skipDuplicate(true).removeOnFailure(false)
                .log(LoggingLevel.INFO, "Consuming postgres log messages as filename: ${header.CamelFileName}")
                .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}")
                .bean(PostgresLogStreamConsumerProcessor.class, "incrementMetrics")
                .bean(InputStreamReader.class, "prepareBodyAsInputStream")
                .bean(PostgresLogStreamConsumerProcessor.class, "decompressIfGzip")
                .to(NATIVE_LOG_STREAM_ENRICHER_ROUTE);
    }
}
