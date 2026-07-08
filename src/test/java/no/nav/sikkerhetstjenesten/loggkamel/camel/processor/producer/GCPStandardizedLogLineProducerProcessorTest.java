package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPStandardizedLogLineProducerProcessor.CLOUD_LOGGING_ENTRY_NAME;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GCPStandardizedLogLineProducerProcessorTest {

    private static final String DATABASE_NAME = "dbName";
    private static final String SQL_STATEMENT = "sql statement";
    private static final TeknologiEnum TEKNOLOGI_IN_MESSAGE = TeknologiEnum.POSTGRESQL;
    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final String PROVIDED_GCP_ID = "gcpId";
    private static final String PROVIDED_FILENAME = "providedFilename";

    @Mock
    private Metrics metrics;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GCPLoggingClientFactory gcpLoggingClientFactory;

    @Mock
    private Logging logging;

    @Mock
    private AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @InjectMocks
    private GCPStandardizedLogLineProducerProcessor processor;

    @Test
    void incrementMetrics_incrementsHappyPathAndDatabaseSpecificMetrics() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEKNOLOGI, TEKNOLOGI_IN_MESSAGE);
        exchange.setVariable(AUDITLOGG_ARKIV, auditloggArkivResponseDTO);
        when(auditloggArkivResponseDTO.getDbname()).thenReturn(DATABASE_NAME);

        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.single, TEKNOLOGI_IN_MESSAGE, Metrics.Action.produced);
        verify(metrics).incrementDatabaseSpecificAction(DATABASE_NAME, TEKNOLOGI_IN_MESSAGE, Metrics.Action.produced);
    }

    @Test
    void writeToGcpLogging_writesInfoEntryToExpectedLogName() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEAM_GCP_PROJECT_ID, PROVIDED_GCP_ID);
        exchange.getMessage().setHeader(FILE_NAME, PROVIDED_FILENAME);
        exchange.getMessage().setBody(EnrichedAuditlogg.builder().dbName(DATABASE_NAME).logTime(NOW).sqlStatement(SQL_STATEMENT).build());

        when(gcpLoggingClientFactory.create(PROVIDED_GCP_ID)).thenReturn(logging);
        Map<String, Object> auditloggAsMap = Map.of("key1", "value1", "key2", "value2");
        when(objectMapper.convertValue(any(EnrichedAuditlogg.class), any(TypeReference.class))).thenReturn(auditloggAsMap);

        processor.writeToGcpLogging(exchange);

        ArgumentCaptor<Iterable> entriesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(logging).write(entriesCaptor.capture());

        Object entryObject = entriesCaptor.getValue().iterator().next();
        LogEntry entry = assertInstanceOf(LogEntry.class, entryObject);
        assertEquals(CLOUD_LOGGING_ENTRY_NAME, entry.getLogName());
        assertEquals(Severity.INFO, entry.getSeverity());
        assertEquals(NOW.toInstant(), entry.getInstantTimestamp());
        assertEquals(DigestUtils.sha256Hex(SQL_STATEMENT), entry.getInsertId());

        Payload.JsonPayload loggedJsonPayload = assertInstanceOf(Payload.JsonPayload.class, entry.getPayload());
        assertEquals(auditloggAsMap, loggedJsonPayload.getDataAsMap());
    }

    @Test
    void writeToGcpLogging_wrapsAnyFailureAsGcpDependencyException() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEAM_GCP_PROJECT_ID, PROVIDED_GCP_ID);
        exchange.setVariable(PLACE_IN_PACKET, 1);
        exchange.getMessage().setHeader(FILE_NAME, PROVIDED_FILENAME);

        when(gcpLoggingClientFactory.create(PROVIDED_GCP_ID)).thenThrow(new RuntimeException("boom"));

        GCPDependencyException exception = assertThrows(GCPDependencyException.class, () -> processor.writeToGcpLogging(exchange));

        assertTrue(exception.getMessage().contains(PROVIDED_FILENAME));
        RuntimeException cause = assertInstanceOf(RuntimeException.class, exception.getCause());
        assertEquals("boom", cause.getMessage());
    }
}
