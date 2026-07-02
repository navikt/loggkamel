package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.LogLineMessageConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggLineErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogLineEnricher.LOG_LINE_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class LogLineMessageConsumer extends LoggLineErrorHandler {

    private static final String KEEP_SOURCE_FILE = "keepSourceFile";

    @Autowired
    @Qualifier("logLineMessageIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    public static String LOG_LINE_MESSAGE_CONSUMER_ID = "log-line-message-consumer";

    @Value("${routing.loggline.bucket}")
    private String consumerUri;

    @Override
    public void configure() {
        // Explicitly delete original local files on route completion. Only necessary when reading from GCP
        if (consumerUri.startsWith("google-storage://")) {
            onCompletion()
                    .onWhen(simple("${exchangeProperty." + KEEP_SOURCE_FILE + "} != true && ${header.CamelDuplicateMessage} != true"))
                    .setHeader(OBJECT_NAME, header(FILE_NAME))
                    .setHeader(GoogleCloudStorageConstants.OPERATION, () -> GoogleCloudStorageOperations.deleteObject)
                    .setBody(constant(null))
                    .log(LoggingLevel.INFO, "Deleting consumed source object ${header.CamelFileName} from consumer bucket")
                    .to(consumerUri);
        }

        super.errorHandling();

        onException(DuplicateKeyException.class)
                .log(LoggingLevel.INFO, "Caught DuplicateKeyException when trying to claim filename: ${headers['CamelFileName']}, aborting processing without removing source file")
                .setProperty(KEEP_SOURCE_FILE, constant(true))
                .handled(true);

        from(consumerUri)
                .routeId(LOG_LINE_MESSAGE_CONSUMER_ID)
                .streamCache(false)
                .autoStartup(false)
                .transacted()
                .bean(LogLineMessageConsumerProcessor.class, "populateFilenameHeader")
                .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName}")
                .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true).removeOnFailure(false)
                .log(LoggingLevel.INFO, "Consuming log messages from ${header.CamelFileName}, converting to AuditloggLineMessage")
                .bean(LogLineMessageConsumerProcessor.class, "mapToAuditloggLineMessage")
                .bean(LogLineMessageConsumerProcessor.class, "incrementMetrics")
                .to(LOG_LINE_ENRICHER_ROUTE);
    }
}
