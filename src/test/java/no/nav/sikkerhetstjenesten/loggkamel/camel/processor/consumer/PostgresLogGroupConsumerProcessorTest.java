package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostgresLogGroupConsumerProcessorTest {

    @Mock
    private Metrics metrics;

    private PostgresLogGroupConsumerProcessor processor;

    @BeforeEach
    void setup() {
        processor = new PostgresLogGroupConsumerProcessor(metrics);
    }

    @Test
    void initializeConsumerState_setsTeknoLogiVariable() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());

        processor.initializeConsumerState(exchange);

        assertEquals(TeknologiEnum.POSTGRESQL, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class));
    }

    @Test
    void initializeConsumerState_populatesFilenameFromObjectNameWhenAbsent() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(OBJECT_NAME, "some-file.auditlog");

        processor.initializeConsumerState(exchange);

        assertEquals("some-file.auditlog", exchange.getIn().getHeader(FILE_NAME, String.class));
    }

    @Test
    void initializeConsumerState_keepsExistingFilenameHeader() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(FILE_NAME, "existing.auditlog");
        exchange.getIn().setHeader(OBJECT_NAME, "other.auditlog");

        processor.initializeConsumerState(exchange);

        assertEquals("existing.auditlog", exchange.getIn().getHeader(FILE_NAME, String.class));
    }

    @Test
    void incrementMetrics_callsHappyPathWithPostgresqlGrouped() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());

        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.grouped, TeknologiEnum.POSTGRESQL, Metrics.Action.consumed);
    }

    @Test
    void decompressIfGzip_doesNothingWhenFilenameHasNoGzExtension() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(FILE_NAME, "some-file.auditlog");
        byte[] originalBody = "plain content".getBytes(StandardCharsets.UTF_8);
        exchange.getMessage().setBody(originalBody);

        processor.decompressIfGzip(exchange);

        assertArrayEquals(originalBody, exchange.getMessage().getBody(byte[].class));
    }

    @Test
    void decompressIfGzip_doesNothingWhenFilenameHeaderIsAbsent() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        byte[] originalBody = "plain content".getBytes(StandardCharsets.UTF_8);
        exchange.getMessage().setBody(originalBody);

        processor.decompressIfGzip(exchange);

        assertArrayEquals(originalBody, exchange.getMessage().getBody(byte[].class));
    }

    @Test
    void decompressIfGzip_decompressesGzipBody() throws Exception {
        String originalContent = "audit log line content";
        byte[] originalContentBytes = originalContent.getBytes(StandardCharsets.UTF_8);
        byte[] gzipBytes = gzip(originalContent);

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(FILE_NAME, "some-file.auditlog.gz");
        exchange.getMessage().setBody(gzipBytes);

        processor.decompressIfGzip(exchange);

        assertArrayEquals(originalContentBytes, exchange.getMessage().getBody(byte[].class));
    }

    @Test
    void decompressIfGzip_throwsInvalidPostgresLogGroupExceptionOnCorruptGzip() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(FILE_NAME, "corrupt-file.auditlog.gz");
        exchange.getMessage().setBody("this is not valid gzip data".getBytes(StandardCharsets.UTF_8));

        InvalidPostgresLogGroupException exception = assertThrows(
            InvalidPostgresLogGroupException.class,
            () -> processor.decompressIfGzip(exchange)
        );

        assertTrue(exception.getMessage().contains("Failed to decompress gzip file corrupt-file.auditlog.gz"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static byte[] gzip(String content) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(buffer)) {
            gzipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return buffer.toByteArray();
    }
}

