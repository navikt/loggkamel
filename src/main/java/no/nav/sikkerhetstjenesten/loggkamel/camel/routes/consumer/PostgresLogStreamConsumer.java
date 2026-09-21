package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogStreamConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.NativeLogStreamEnricher.NATIVE_LOG_STREAM_ENRICHER_ROUTE;

public abstract class PostgresLogStreamConsumer extends LogStreamErrorHandler {

    protected static final String KEEP_SOURCE_FILE = "keepSourceFile";

    protected final PostgresLogStreamConsumerProcessor consumerProcessor;
    private final InputStreamReader inputStreamReader;

    public static final String POSTGRES_LOG_CONSUMER_ID = "postgres-log-stream-consumer";

    @Autowired
    @Qualifier("postgresLogStreamIdempotentRepository")
    private JdbcMessageIdRepository postgresLogStreamIdempotentRepository;

    protected PostgresLogStreamConsumer(
            PostgresLogStreamConsumerProcessor consumerProcessor,
            InputStreamReader inputStreamReader
    ) {
        this.consumerProcessor = consumerProcessor;
        this.inputStreamReader = inputStreamReader;
    }

    protected void configureConsumer(String postgresStreamConsumerUri, Processor envSpecificStateInitializer) {
        this.errorHandling();

        onException(DuplicateKeyException.class)
                .log(LoggingLevel.INFO, "Caught DuplicateKeyException when trying to claim filename: ${header.LoggkamelFilename}, aborting processing without removing source file")
                .setProperty(KEEP_SOURCE_FILE, constant(true))
                .handled(true);

        from(postgresStreamConsumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .streamCache(false)
                .autoStartup(false)
                .transacted()
                .process(envSpecificStateInitializer)
                //Prevent multiple instances of loggkamel from processing the same file, leave removal of the file up to the instance processing it
                .idempotentConsumer(header(LOG_FILENAME), postgresLogStreamIdempotentRepository).skipDuplicate(true).removeOnFailure(false)
                .log(LoggingLevel.INFO, "Consuming postgres log messages as filename: ${header.LoggkamelFilename}")
                .process(consumerProcessor::incrementMetrics)
                .process(inputStreamReader::prepareBodyAsInputStream)
                .process(consumerProcessor::decompressIfGzip)
                .to(NATIVE_LOG_STREAM_ENRICHER_ROUTE);
    }
}
