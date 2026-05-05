package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

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

    //HERE FOR DEBUG, REMOVE
    /**
     * Utility method to capture body metadata for diagnostics
     */
    private void captureBodyMetadata(Exchange exchange, String phase) {
        try {
            Object body = exchange.getIn().getBody();
            String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);

            String bodyType = body != null ? body.getClass().getSimpleName() : "null";
            long bodySize = 0;
            String bodyHash = "N/A";

            if (body instanceof String) {
                bodySize = ((String) body).length();
                bodyHash = computeHash(((String) body).getBytes());
            } else if (body instanceof byte[]) {
                bodySize = ((byte[]) body).length;
                bodyHash = computeHash((byte[]) body);
            } else if (body instanceof InputStream) {
                bodyType = "InputStream - NOT FULLY BUFFERED YET";
                bodySize = -1;
                bodyHash = "STREAM_NOT_BUFFERED";
            }

            log.info("[DIAGNOSTIC-{}] File: {}, Body Type: {}, Body Size: {} bytes, Body Hash: {}",
                    phase, fileName, bodyType, bodySize, bodyHash);

            // Store for later comparison
            if ("PRE_DECOMPRESS".equals(phase)) {
                exchange.setVariable(BODY_TYPE_PRE_DECOMPRESS, bodyType);
                exchange.setVariable(BODY_SIZE_PRE_DECOMPRESS, bodySize);
                exchange.setVariable(BODY_HASH_PRE_DECOMPRESS, bodyHash);
            } else if ("POST_DECOMPRESS".equals(phase)) {
                exchange.setVariable(BODY_TYPE_POST_DECOMPRESS, bodyType);
                exchange.setVariable(BODY_SIZE_POST_DECOMPRESS, bodySize);
                exchange.setVariable(BODY_HASH_POST_DECOMPRESS, bodyHash);

                // Log comparison
                String preType = exchange.getVariable(BODY_TYPE_PRE_DECOMPRESS, String.class);
                String preSize = String.valueOf(exchange.getVariable(BODY_SIZE_PRE_DECOMPRESS, long.class));
                String postSize = String.valueOf(bodySize);
                log.info("[DIAGNOSTIC-DECOMPRESS_COMPARISON] Type changed: {} -> {}, Size: {} -> {} bytes",
                        preType, bodyType, preSize, postSize);
            }
        } catch (Exception e) {
            log.warn("[DIAGNOSTIC] Error capturing body metadata: {}", e.getMessage(), e);
        }
    }

    @Override
    public void configure() {
        super.errorHandling();

        from(consumerUri)
            .routeId(POSTGRES_LOG_CONSUMER_ID)
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
                    .process(exchange -> {
                        log.info("[DIAGNOSTIC-PRE_DECOMPRESS] About to decompress file: {}",
                            exchange.getIn().getHeader(FILE_NAME, String.class));
                        captureBodyMetadata(exchange, "PRE_DECOMPRESS");
                    })
                    // if log file is compressed, decompress and remove the compression extension from the filename
                    .doTry()
                        .unmarshal().gzipDeflater()
                        .process(exchange -> {
                            log.info("[DIAGNOSTIC-POST_DECOMPRESS] Decompression completed for file: {}",
                                exchange.getIn().getHeader(FILE_NAME, String.class));
                            captureBodyMetadata(exchange, "POST_DECOMPRESS");
                        })
                        .endDoTry()
                    .doCatch(Exception.class)
                        .process(exchange -> {
                            String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                            Exception cause = exchange.getProperty(EXCEPTION_CAUGHT, Exception.class);
                            String errorMessage = cause != null ? cause.getMessage() : "unknown error";

                            log.error("[DIAGNOSTIC-DECOMPRESS_FAILURE] Decompression failed for file: {}, Error: {}",
                                fileName, errorMessage, cause);
                            captureBodyMetadata(exchange, "POST_DECOMPRESS_FAILURE");

                            throw new InvalidPostgresLogGroupException(
                                "Failed to decompress gzip file " + (fileName != null ? fileName : "unknown") + ", error: " + errorMessage
                            );
                        })
                    .end()
                    .process(exchange -> {
                        String originalFileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                        String newFileName = originalFileName.substring(0, originalFileName.length() - 3);

                        exchange.getIn().setHeader(FILE_NAME, newFileName);
                    })
                .end()
            .to(LOG_GROUP_ENRICHER_ROUTE);
    }
}
