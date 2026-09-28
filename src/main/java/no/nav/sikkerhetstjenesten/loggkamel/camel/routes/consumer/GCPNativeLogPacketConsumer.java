package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import com.google.cloud.logging.Logging;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
@ConditionalOnGCP
public class GCPNativeLogPacketConsumer extends NativeLogPacketConsumer {

    @Value("${routing.packet.bucket}")
    private String logPacketConsumerUri;

    public GCPNativeLogPacketConsumer(
            NativeLogPacketConsumerProcessor consumerProcessor,
            InputStreamReader inputStreamReader,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        super(consumerProcessor, inputStreamReader, routeConfigurationIdResolver);
    }

    @Override
    public void configure() {
        onCompletion()
                .onWhen(simple("${exchangeProperty." + KEEP_SOURCE_FILE + "} != true && ${header.CamelDuplicateMessage} != true"))
                .process(exchange -> {
                    Logging logging = exchange.getVariable(NativeLogPacketConsumerProcessor.LOGGING_CLIENT, Logging.class);
                    if (logging == null) {
                        log.warn("No logging client found for packet {}, cannot flush or close. Possible loss of logs",
                                exchange.getMessage().getHeader(LOG_FILENAME));
                        return;
                    }
                    logging.flush();
                    logging.close();
                })
                .setHeader(OBJECT_NAME, header(LOG_FILENAME))
                .setHeader(GoogleCloudStorageConstants.OPERATION, () -> GoogleCloudStorageOperations.deleteObject)
                .setBody(constant((Object) null))
                .log(LoggingLevel.INFO, "Deleting consumed source object ${header.LoggkamelFilename} from consumer bucket")
                .to(logPacketConsumerUri);

        configureConsumer(logPacketConsumerUri, consumerProcessor::populateGCPFilenameHeader);
    }
}
