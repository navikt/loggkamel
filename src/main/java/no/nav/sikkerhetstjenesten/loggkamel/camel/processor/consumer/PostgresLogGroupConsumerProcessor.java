package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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

        log.info("Log file {} is gzip compressed, wrapping body in GZIPInputStream for streaming decompression", fileName);
        try {
            InputStream compressedStream = exchange.getMessage().getBody(InputStream.class);
            GZIPInputStream gzipInputStream = new GZIPInputStream(compressedStream);
            exchange.getMessage().setBody(gzipInputStream);
        } catch (IOException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "unknown error";
            throw new InvalidPostgresLogGroupException(
                "Failed to open gzip stream for file " + fileName + ", error: " + errorMessage, e
            );
        }
    }
}

