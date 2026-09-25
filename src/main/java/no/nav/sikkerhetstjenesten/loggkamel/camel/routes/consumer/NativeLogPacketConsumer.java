package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.ERROR_METRIC_MULTIPLICITY;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.NativeLogLineEnricherAssigner.NATIVE_LOG_LINE_ENRICHER_ROUTE;

public abstract class NativeLogPacketConsumer extends RouteBuilder {

    protected static final String KEEP_SOURCE_FILE = "keepSourceFile";

    protected final NativeLogPacketConsumerProcessor consumerProcessor;
    private final InputStreamReader inputStreamReader;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    @Autowired
    @Qualifier("logPacketIdempotentRepository")
    private JdbcMessageIdRepository logPacketIdempotentRepository;

    public static final String NATIVE_LOG_PACKET_CONSUMER_ID = "native-log-packet-consumer";

    protected NativeLogPacketConsumer(
            NativeLogPacketConsumerProcessor consumerProcessor,
            InputStreamReader inputStreamReader,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.consumerProcessor = consumerProcessor;
        this.inputStreamReader = inputStreamReader;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    protected void configureConsumer(String logPacketConsumerUri, Processor filenameInitializer) {
        onException(DuplicateKeyException.class)
                .log(LoggingLevel.INFO, "Caught DuplicateKeyException when trying to claim filename: ${header.LoggkamelFilename}, aborting processing without removing source file")
                .setProperty(KEEP_SOURCE_FILE, constant(true))
                .handled(true);

        from(logPacketConsumerUri)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.PACKET))
                .routeId(NATIVE_LOG_PACKET_CONSUMER_ID)
                .setProperty(ERROR_METRIC_MULTIPLICITY, constant(Metrics.Multiplicity.packet))
                .streamCache(false)
                .autoStartup(false)
                .transacted()
                .process(filenameInitializer)
                .idempotentConsumer(header(LOG_FILENAME), logPacketIdempotentRepository).skipDuplicate(true).removeOnFailure(false)
                .log(LoggingLevel.INFO, "Consuming log messages from ${header.LoggkamelFilename}, converting to AuditloggLineMessage")
                .process(inputStreamReader::prepareBodyAsInputStream)
                .process(consumerProcessor::mapToLogLineList)
                .process(consumerProcessor::initializeExchangeVariablesForPacket)
                .process(consumerProcessor::incrementMetricsForPacket)
                .split(body())
                    .process(consumerProcessor::initializeExchangeVariablesForLogLine)
                    .process(consumerProcessor::incrementMetricsForLine)
                    .to(NATIVE_LOG_LINE_ENRICHER_ROUTE);
    }
}
