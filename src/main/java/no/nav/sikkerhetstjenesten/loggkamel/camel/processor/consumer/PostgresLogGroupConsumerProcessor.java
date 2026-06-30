package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Service
public class PostgresLogGroupConsumerProcessor {

    private static final Logger log = LoggerFactory.getLogger(PostgresLogGroupConsumerProcessor.class);

    private final Metrics metrics;

    @Autowired
    public PostgresLogGroupConsumerProcessor(Metrics metrics) {
        this.metrics = metrics;
    }

    public void initializeConsumerState(Exchange exchange) {
        exchange.setVariable(TEKNOLOGI, TeknologiEnum.POSTGRESQL);
        if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
            exchange.getIn().setHeader(FILE_NAME, exchange.getIn().getHeader(OBJECT_NAME, String.class));
        }
    }

    public void incrementMetrics(Exchange exchange) {
        metrics.incrementHappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.Action.consumed);
    }

    public void decompressIfGzip(Exchange exchange) {
        String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);
        if (fileName == null || !fileName.endsWith(".gz")) {
            return;
        }

        log.debug("Log file {} is gzip compressed, attempting to decompress", fileName);
        try {
            byte[] compressedBytes = exchange.getMessage().getBody(byte[].class);
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = gzipInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                exchange.getMessage().setBody(outputStream.toByteArray());
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "unknown error";
            throw new InvalidPostgresLogGroupException(
                "Failed to decompress gzip file " + fileName + ", error: " + errorMessage, e
            );
        }
    }
}

