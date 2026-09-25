package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.model.RouteConfigurationDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.TEKNOLOGI;

@Component
public class LogRouteConfiguration extends RouteConfigurationBuilder {

    public static final String LOCAL_STREAM = "local-stream";
    public static final String LOCAL_PACKET = "local-packet";
    public static final String GCP_STREAM = "gcp-stream";
    public static final String GCP_PACKET = "gcp-packet";
    public static final String ERROR_METRIC_MULTIPLICITY = "ErrorMetricMultiplicity";
    public static final String ORIGINAL_FILENAME = "originalFilename";

    // Local: the directory a backed-out stream is written to directly.
    @Value("${routing.postgres.invalid-message}")
    private String postgresLocalBackoutDirectoryUri;

    // GCP: the bare bucket name a backed-out stream is copied to (used as a GCS header value, not an endpoint).
    @Value("${routing.postgres.invalid-message}")
    private String postgresBackoutBucketName;

    // GCP: the consumer bucket a backed-out stream is copied FROM. We send the copy command here because
    // the source object still lives in the consumer bucket at the time an exception is handled.
    @Value("${routing.postgres.consumer}")
    private String postgresConsumerUri;

    // Local: the directory a backed-out packet is written to directly.
    @Value("${routing.packet.invalid-message}")
    private String packetLocalBackoutDirectoryUri;

    // GCP: the bare bucket name a backed-out packet is copied to (used as a GCS header value, not an endpoint).
    @Value("${routing.packet.invalid-message}")
    private String packetBackoutBucketName;

    // GCP: the consumer bucket a backed-out packet is copied FROM.
    @Value("${routing.packet.bucket}")
    private String packetConsumerUri;

    private final Metrics metrics;

    public LogRouteConfiguration(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void configuration() {
        // Use of original message allowed for backouts when running locally, stream caching disabled to avoid loading large streams into memory
        getContext().setAllowUseOriginalMessage(true);
        getContext().setStreamCaching(false);

        // Local: no GCS copy is needed, the backed-out body is written straight to the backout directory.
        configureStream(routeConfiguration(LOCAL_STREAM), exchange -> { }, postgresLocalBackoutDirectoryUri);
        configurePacket(routeConfiguration(LOCAL_PACKET), exchange -> { }, packetLocalBackoutDirectoryUri);

        // GCP: the message is sent back to the consumer bucket with a header instructing it to copy the
        // original object to the backout bucket; the consumer bucket then performs the copy.
        configureStream(
                routeConfiguration(GCP_STREAM),
                exchange -> convertMessageToGCPCopyRequest(exchange, exchange.getMessage().getHeader(ORIGINAL_FILENAME), postgresBackoutBucketName),
                postgresConsumerUri
        );
        configurePacket(
                routeConfiguration(GCP_PACKET),
                exchange -> convertMessageToGCPCopyRequest(exchange, exchange.getMessage().getHeader(LOG_FILENAME), packetBackoutBucketName),
                packetConsumerUri
        );
    }

    private void configureStream(RouteConfigurationDefinition configuration, Processor prepareBackout, String backoutEndpointUri) {
        configuration.onException(DependencyException.class)
                .log(LoggingLevel.INFO, "Routing DependencyException to postgres invalid-messages channel after retries: ${exception.message}, filename: ${header.LoggkamelFilename}")
                .maximumRedeliveries(3)
                .redeliveryDelay(10000)
                .handled(true)
                .useOriginalBody()
                .process(exchange -> metrics.incrementBackoutQueueMetrics(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL))
                .process(prepareBackout)
                .to(backoutEndpointUri);

        configuration.onException(InvalidLogException.class)
                .log(LoggingLevel.INFO, "Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${header.LoggkamelFilename}")
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody()
                .process(exchange -> metrics.incrementBackoutQueueMetrics(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL))
                .process(prepareBackout)
                .to(backoutEndpointUri);

        configuration.onException(Exception.class)
                .log(LoggingLevel.WARN, "Routing unhandled exception to postgres invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${header.LoggkamelFilename}")
                .log(LoggingLevel.DEBUG, "Exception stack trace: ${exception.stacktrace}")
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody()
                .process(exchange -> metrics.incrementBackoutQueueMetrics(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL))
                .process(prepareBackout)
                .to(backoutEndpointUri);
    }

    private void configurePacket(RouteConfigurationDefinition configuration, Processor prepareBackout, String backoutEndpointUri) {
        configuration.onException(DependencyException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(10000)
                .handled(true)
                .useOriginalBody()
                .log(LoggingLevel.INFO, "Routing DependencyException to invalid-messages channel after retries: ${exception.message}, filename: ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(this::incrementPacketBackoutMetric)
                .process(prepareBackout)
                .to(backoutEndpointUri);

        configuration.onException(InvalidLogException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody()
                .log(LoggingLevel.INFO, "Routing InvalidLogException to invalid-messages channel: ${exception.message}, filename: ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(this::incrementPacketBackoutMetric)
                .process(prepareBackout)
                .to(backoutEndpointUri);

        configuration.onException(Exception.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalBody()
                .log(LoggingLevel.WARN, "Routing unhandled exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${header.LoggkamelFilename} line ${variable.PlaceInPacket}")
                .process(this::incrementPacketBackoutMetric)
                .process(prepareBackout)
                .to(backoutEndpointUri);
    }

    private void incrementPacketBackoutMetric(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        Metrics.Multiplicity multiplicity = exchange.getProperty(
                ERROR_METRIC_MULTIPLICITY,
                Metrics.Multiplicity.packet,
                Metrics.Multiplicity.class
        );
        metrics.incrementBackoutQueueMetrics(
                multiplicity,
                teknologi != null ? teknologi : TeknologiEnum.UNKNOWN
        );
    }

    private void convertMessageToGCPCopyRequest(Exchange exchange, Object filename, String destinationBucket) {
        exchange.getMessage().setHeader(GoogleCloudStorageConstants.OPERATION, GoogleCloudStorageOperations.copyObject);
        exchange.getMessage().setHeader(GoogleCloudStorageConstants.OBJECT_NAME, filename);
        exchange.getMessage().setHeader(GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, destinationBucket);
        exchange.getMessage().setHeader(GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, filename);
        exchange.getMessage().setBody(null);
    }
}
