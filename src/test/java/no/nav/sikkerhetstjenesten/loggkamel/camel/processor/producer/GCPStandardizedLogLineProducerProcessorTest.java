package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogPacketException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.util.GCPTimestampProvider;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor.PENDING_LOG_ENTRIES;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPStandardizedLogLineProducerProcessor.CLOUD_LOGGING_ENTRY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GCPStandardizedLogLineProducerProcessorTest {

    private static final String DATABASE_NAME = "dbName";
    private static final String SQL_STATEMENT = "sql statement";
    private static final String SQL_PARAMETERS = "sql parameters";
    private static final TeknologiEnum TEKNOLOGI_IN_MESSAGE = TeknologiEnum.POSTGRESQL;
    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final String PROVIDED_FILENAME = "providedFilename";

    @Mock
    private Metrics metrics;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GCPTimestampProvider  gcpTimestampProvider;

    @Mock
    private AuditloggTaskDTO auditloggTaskDTO;

    @InjectMocks
    private GCPStandardizedLogLineProducerProcessor processor;

    @Test
    void incrementMetrics_incrementsHappyPathAndDatabaseSpecificMetrics() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEKNOLOGI, TEKNOLOGI_IN_MESSAGE);
        exchange.setVariable(AUDITLOGG_TASK, auditloggTaskDTO);
        when(auditloggTaskDTO.getDbname()).thenReturn(DATABASE_NAME);

        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.line, TEKNOLOGI_IN_MESSAGE, Metrics.Action.produced);
        verify(metrics).incrementDatabaseSpecificAction(DATABASE_NAME, TEKNOLOGI_IN_MESSAGE, Metrics.Action.produced);
    }

    @Test
    void queueLogEntry_exceptionOnNullMessage() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(PENDING_LOG_ENTRIES, new ArrayList<LogEntry>());

        assertThrows(InvalidLogPacketException.class, () -> processor.queueLogEntry(exchange));
    }

    @Test
    void queueLogEntry_exceptionOnMissingPendingLogEntries() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody(EnrichedAuditlogg.builder().build());

        assertThrows(InvalidLogPacketException.class, () -> processor.queueLogEntry(exchange));
    }

    @Test
    void queueLogEntry_queuesInfoEntryWithExpectedLogName() {
        List<LogEntry> pendingLogEntries = new ArrayList<>();
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(PENDING_LOG_ENTRIES, pendingLogEntries);
        exchange.getMessage().setHeader(LOG_FILENAME, PROVIDED_FILENAME);
        exchange.getMessage().setBody(EnrichedAuditlogg.builder()
                .dbName(DATABASE_NAME)
                .logTime(NOW)
                .sqlStatement(SQL_STATEMENT)
                .sqlParameters(SQL_PARAMETERS).build());

        Map<String, Object> auditloggAsMap = Map.of("key1", "value1", "key2", "value2");
        when(objectMapper.convertValue(any(EnrichedAuditlogg.class), any(TypeReference.class))).thenReturn(auditloggAsMap);
        when(gcpTimestampProvider.getTimestampFromLogTime(NOW)).thenReturn(NOW.toInstant());

        processor.queueLogEntry(exchange);

        assertEquals(1, pendingLogEntries.size());
        LogEntry entry = pendingLogEntries.getFirst();
        assertEquals(CLOUD_LOGGING_ENTRY_NAME, entry.getLogName());
        assertEquals(Severity.INFO, entry.getSeverity());
        assertEquals(NOW.toInstant(), entry.getInstantTimestamp());
        assertEquals(DigestUtils.sha256Hex( SQL_STATEMENT + SQL_PARAMETERS), entry.getInsertId());

        Payload.JsonPayload loggedJsonPayload = assertInstanceOf(Payload.JsonPayload.class, entry.getPayload());
        assertEquals(auditloggAsMap, loggedJsonPayload.getDataAsMap());
    }
}
