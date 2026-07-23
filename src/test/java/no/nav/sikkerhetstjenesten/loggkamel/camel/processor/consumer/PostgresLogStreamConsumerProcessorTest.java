package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogStreamConsumerProcessor.COMPRESSION_EXTENSION;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler.ORIGINAL_FILENAME;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresLogStreamConsumerProcessorTest {

    private static final String DESIRED_FILENAME = "desiredFilename";
    private static final String COMPRESSED_FILENAME = DESIRED_FILENAME + COMPRESSION_EXTENSION;

    @Mock
    private Metrics metrics;

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @InjectMocks
    PostgresLogStreamConsumerProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(exchange.getMessage()).thenReturn(message);
    }

    @Test
    void initializeConsumerState_setsTeknoLogiVariable() {
        processor.initializeConsumerState(exchange);

        verify(exchange).setVariable(TEKNOLOGI, TeknologiEnum.POSTGRESQL);
    }

    @Test
    void initializeConsumerState_populatesFilenameFromObjectNameWhenAbsent() {
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(null);
        when(message.getHeader(OBJECT_NAME, String.class)).thenReturn(DESIRED_FILENAME);

        processor.initializeConsumerState(exchange);

        verify(message).setHeader(FILE_NAME, DESIRED_FILENAME);
    }

    @Test
    void initializeConsumerState_setOriginalFilename() {
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(DESIRED_FILENAME);

        processor.initializeConsumerState(exchange);

        verify(message).setHeader(ORIGINAL_FILENAME, DESIRED_FILENAME);
        verifyNoMoreInteractions(message);
    }

    @Test
    void incrementMetrics_callsHappyPathWithPostgresqlStream() {
        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.stream, TeknologiEnum.POSTGRESQL, Metrics.Action.consumed);
    }

    @Test
    void decompressIfGzip_doesNothingWhenFilenameHasNoGzExtension() {
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(DESIRED_FILENAME);

        processor.decompressIfGzip(exchange);

        verify(exchange).getMessage();
        verifyNoMoreInteractions(exchange, message);
    }

    @Test
    void decompressIfGzip_doesNothingWhenFilenameHeaderIsAbsent() {
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(null);

        processor.decompressIfGzip(exchange);

        verify(exchange).getMessage();
        verifyNoMoreInteractions(exchange, message);
    }

    @Test
    void decompressIfGzip_setsBodyToGZIPInputStreamAndUpdatesHeader() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(gzip("audit log line content"));

        when(message.getHeader(FILE_NAME, String.class)).thenReturn(COMPRESSED_FILENAME);
        when(message.getBody(InputStream.class)).thenReturn(inputStream);

        processor.decompressIfGzip(exchange);

        verify(message).setBody(any(GZIPInputStream.class));
        verify(message).setHeader(FILE_NAME, DESIRED_FILENAME);
    }

    @Test
    void decompressIfGzip_gzipStreamDecompressesCorrectlyWhenRead() throws Exception {
        String originalContent = "audit log line content";
        InputStream inputStream = new ByteArrayInputStream(gzip(originalContent));

        when(message.getHeader(FILE_NAME, String.class)).thenReturn(COMPRESSED_FILENAME);
        when(message.getBody(InputStream.class)).thenReturn(inputStream);

        processor.decompressIfGzip(exchange);

        ArgumentCaptor<GZIPInputStream> gzipInputStreamArgumentCaptor = ArgumentCaptor.forClass(GZIPInputStream.class);
        verify(message, times(1)).setBody(gzipInputStreamArgumentCaptor.capture());

        byte[] decompressedBytes = gzipInputStreamArgumentCaptor.getValue().readAllBytes();
        assertArrayEquals(originalContent.getBytes(StandardCharsets.UTF_8), decompressedBytes);
    }

    @Test
    void decompressIfGzip_throwsInvalidPostgresLogStreamExceptionOnCorruptGzip() {
        InputStream inputStream = new ByteArrayInputStream("this is not valid gzip data".getBytes(StandardCharsets.UTF_8));

        when(message.getHeader(FILE_NAME, String.class)).thenReturn(COMPRESSED_FILENAME);
        when(message.getBody(InputStream.class)).thenReturn(inputStream);

        InvalidPostgresLogStreamException exception = assertThrows(
            InvalidPostgresLogStreamException.class,
            () -> processor.decompressIfGzip(exchange)
        );

        assertTrue(exception.getMessage().contains("Failed to open gzip stream for file"));
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

