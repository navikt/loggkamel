package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogStreamConsumerProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler.ORIGINAL_FILENAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
@ConditionalOnGCP
public class GCPPostgresLogStreamConsumer extends PostgresLogStreamConsumer {

    @Value("${routing.postgres.consumer}")
    private String postgresStreamConsumerUri;

    public GCPPostgresLogStreamConsumer(
            PostgresLogStreamConsumerProcessor consumerProcessor,
            InputStreamReader inputStreamReader
    ) {
        super(consumerProcessor, inputStreamReader);
    }

    @Override
    public void configure() {
        onCompletion()
                .onWhen(simple("${exchangeProperty." + KEEP_SOURCE_FILE + "} != true && ${header.CamelDuplicateMessage} != true"))
                .setHeader(OBJECT_NAME, header(ORIGINAL_FILENAME))
                .setHeader(GoogleCloudStorageConstants.OPERATION, () -> GoogleCloudStorageOperations.deleteObject)
                .setBody(constant((Object) null))
                .log(LoggingLevel.INFO, "Deleting consumed source object ${header.originalFilename} from consumer bucket")
                .to(postgresStreamConsumerUri);

        configureConsumer(postgresStreamConsumerUri, consumerProcessor::initializeGCPConsumerState);
    }
}
