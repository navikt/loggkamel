package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import com.google.cloud.logging.Logging;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.NativeLogLineEnricherAssigner.NATIVE_LOG_LINE_ENRICHER_ROUTE;
import static no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig.GCP_LOGGING_BY_ID;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class NativeLogPacketConsumer extends LogPacketErrorHandler {

    private static final String KEEP_SOURCE_FILE = "keepSourceFile";

    @Autowired
    @Qualifier("logPacketIdempotentRepository")
    private JdbcMessageIdRepository logPacketIdempotentRepository;

    @Autowired
    private CacheManager cacheManager;

    public static String NATIVE_LOG_PACKET_CONSUMER_ID = "native-log-packet-consumer";

    @Value("${routing.packet.bucket}")
    private String logPacketConsumerUri;

    @Override
    public void configure() {
        // Explicitly delete original local files on route completion. Only necessary when reading from GCP
        if (logPacketConsumerUri.startsWith("google-storage://")) {
            onCompletion()
                    .onWhen(simple("${exchangeProperty." + KEEP_SOURCE_FILE + "} != true && ${header.CamelDuplicateMessage} != true"))
                    //Flush Logging object for the GCP project to ensure all logs write successfully before deleting source packet
                    .process(exchange -> {
                        String gcpId = exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class);
                        if (gcpId != null) {
                            try {
                                cacheManager.getCache(GCP_LOGGING_BY_ID).get(gcpId, Logging.class).flush();
                            } catch (Exception e) {
                                throw new GCPDependencyException("Failed to flush GCP logging client for project ID: " + gcpId, e);
                            }
                        } else {
                            log.warn("Cannot flush GCP Logging for object ${header.CamelFileName}, as project ID in header is null");
                        }
                    })
                    .setHeader(OBJECT_NAME, header(FILE_NAME))
                    .setHeader(GoogleCloudStorageConstants.OPERATION, () -> GoogleCloudStorageOperations.deleteObject)
                    .setBody(constant(null))
                    .log(LoggingLevel.INFO, "Deleting consumed source object ${header.CamelFileName} from consumer bucket")
                    .to(logPacketConsumerUri);
        }

        super.errorHandling();

        onException(DuplicateKeyException.class)
                .log(LoggingLevel.INFO, "Caught DuplicateKeyException when trying to claim filename: ${headers['CamelFileName']}, aborting processing without removing source file")
                .setProperty(KEEP_SOURCE_FILE, constant(true))
                .handled(true);

        from(logPacketConsumerUri)
                .routeId(NATIVE_LOG_PACKET_CONSUMER_ID)
                .streamCache(false)
                .autoStartup(false)
                .transacted()
                .bean(NativeLogPacketConsumerProcessor.class, "populateFilenameHeader")
                .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName}")
                .idempotentConsumer(header(FILE_NAME), logPacketIdempotentRepository).skipDuplicate(true).removeOnFailure(false)
                .log(LoggingLevel.INFO, "Consuming log messages from ${header.CamelFileName}, converting to AuditloggLineMessage")
                .bean(NativeLogPacketConsumerProcessor.class, "mapToLogLineList")
                .split(body())
                    .bean(NativeLogPacketConsumerProcessor.class, "initializeExchangeVariablesFromLogLine")
                    .bean(NativeLogPacketConsumerProcessor.class, "incrementMetrics")
                    .to(NATIVE_LOG_LINE_ENRICHER_ROUTE);
    }
}
