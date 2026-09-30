package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Synchronicity;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogPacketException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor.LOGGING_CLIENT;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor.PENDING_LOG_ENTRIES;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.ORIGINAL_FILE_PATH;
import static no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum.POSTGRESQL;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.apache.camel.component.file.FileConstants.FILE_ABSOLUTE_PATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NativeLogPacketConsumerProcessorTest {

    private final static String NAME_FROM_CAMEL = "nameFromCamel";
    private final static String NAME_FROM_BUCKET = "nameFromBucket";
    private static final String TEAM_PROJECT_ID = "projectId";
    private static final String DB_NAME = "dbName";
    private static final Integer PLACE_IN_PACKET_INT = 2;

    @Mock
    private AuditloggLineMessage auditloggLineMessage;

    @Mock
    private AuditloggLineMessageHeader auditloggLineMessageHeader;

    @Mock
    private AuditloggTaskDTO auditloggTaskDTO;

    @Mock
    private Metrics metrics;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Logging logging;

    @InjectMocks
    private NativeLogPacketConsumerProcessor processor;

    @Test
    void populateGCPFilenameHeader_usesObjectName() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader(OBJECT_NAME, NAME_FROM_BUCKET);

        processor.populateGCPFilenameHeader(exchange);

        assertEquals(NAME_FROM_BUCKET, exchange.getMessage().getHeader(LOG_FILENAME, String.class));
    }

    @Test
    void populateLocalFilenameHeader_usesCamelFileName() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        String sourcePath = "/tmp/" + NAME_FROM_CAMEL;
        exchange.getMessage().setHeader(FILE_NAME, NAME_FROM_CAMEL);
        exchange.getMessage().setHeader(FILE_ABSOLUTE_PATH, sourcePath);

        processor.populateLocalFilenameHeader(exchange);

        assertEquals(NAME_FROM_CAMEL, exchange.getMessage().getHeader(LOG_FILENAME, String.class));
        assertEquals(sourcePath, exchange.getProperty(ORIGINAL_FILE_PATH, String.class));
    }

    @Test
    void mapToLogLineList_setsBody() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody("blah");
        when(objectMapper.readValue(eq("blah"), any(TypeReference.class))).thenReturn(List.of(auditloggLineMessage));

        processor.mapToLogLineList(exchange);

        assertInstanceOf(List.class, exchange.getMessage().getBody());
        assertInstanceOf(AuditloggLineMessage.class, exchange.getMessage().getBody(List.class).get(0));
        assertEquals(auditloggLineMessage, exchange.getMessage().getBody(List.class).get(0));
    }

    @Test
    void initializeExchangeVariablesForPacket_setsVariables() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody(List.of(auditloggLineMessage));
        when(auditloggLineMessage.getHeader()).thenReturn(auditloggLineMessageHeader);
        when(auditloggLineMessageHeader.getTeamGcpProjectId()).thenReturn(TEAM_PROJECT_ID);
        when(auditloggLineMessageHeader.getTeknologi()).thenReturn(POSTGRESQL);

        processor.initializeExchangeVariablesForPacket(exchange);

        assertNotNull(exchange.getVariable(LOGGING_CLIENT, Logging.class));
        assertNotNull(exchange.getVariable(LOGGING_CLIENT, Logging.class).getOptions());
        assertEquals(TEAM_PROJECT_ID, exchange.getVariable(LOGGING_CLIENT, Logging.class).getOptions().getProjectId());
        assertEquals(Synchronicity.SYNC, exchange.getVariable(LOGGING_CLIENT, Logging.class).getWriteSynchronicity());
        assertEquals(List.of(), exchange.getVariable(PENDING_LOG_ENTRIES, List.class));
        assertEquals(POSTGRESQL, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class));
    }

    @Test
    void writePendingLogEntries_writesAllEntriesInOneCall() {
        List<LogEntry> pendingLogEntries = List.of(LogEntry.of(Payload.StringPayload.of("a")), LogEntry.of(Payload.StringPayload.of("b")));
        Exchange exchange = packetExchange(pendingLogEntries);

        processor.writePendingLogEntries(exchange);

        verify(logging).write(pendingLogEntries);
    }

    @Test
    void writePendingLogEntries_skipsWriteWhenNoEntriesArePending() {
        Exchange exchange = packetExchange(List.of());

        processor.writePendingLogEntries(exchange);

        verifyNoInteractions(logging);
    }

    @Test
    void writePendingLogEntries_wrapsWriteFailureAsGcpDependencyException() {
        Exchange exchange = packetExchange(List.of(LogEntry.of(Payload.StringPayload.of("a"))));
        doThrow(new RuntimeException("boom")).when(logging).write(any());

        GCPDependencyException exception = assertThrows(GCPDependencyException.class, () -> processor.writePendingLogEntries(exchange));

        assertTrue(exception.getMessage().contains(NAME_FROM_BUCKET));
        assertEquals("boom", exception.getCause().getMessage());
    }

    @Test
    void writePendingLogEntries_failsWhenLoggingClientIsMissing() {
        Exchange exchange = packetExchange(List.of());
        exchange.removeVariable(LOGGING_CLIENT);

        assertThrows(InvalidLogPacketException.class, () -> processor.writePendingLogEntries(exchange));
    }

    @Test
    void closeLoggingClient_closesClient() throws Exception {
        processor.closeLoggingClient(packetExchange(List.of()));

        verify(logging).close();
    }

    @Test
    void closeLoggingClient_toleratesMissingClientAndCloseFailure() throws Exception {
        assertDoesNotThrow(() -> processor.closeLoggingClient(new DefaultExchange(new DefaultCamelContext())));

        doThrow(new RuntimeException("boom")).when(logging).close();
        assertDoesNotThrow(() -> processor.closeLoggingClient(packetExchange(List.of())));
    }

    @Test
    void incrementBackoutFailureMetric_incrementsPacketBackoutFailure() {
        processor.incrementBackoutFailureMetric(new DefaultExchange(new DefaultCamelContext()));

        verify(metrics).incrementBackoutFailure(Metrics.Multiplicity.packet);
    }

    private Exchange packetExchange(List<LogEntry> pendingLogEntries) {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader(LOG_FILENAME, NAME_FROM_BUCKET);
        exchange.setVariable(LOGGING_CLIENT, logging);
        exchange.setVariable(PENDING_LOG_ENTRIES, pendingLogEntries);
        return exchange;
    }

    @Test
    void initializeExchangeVariablesForLogLine_setsVariables() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody(auditloggLineMessage);
        when(auditloggLineMessage.getHeader()).thenReturn(auditloggLineMessageHeader);
        when(auditloggLineMessageHeader.getTeknologi()).thenReturn(POSTGRESQL);
        when(auditloggLineMessageHeader.getAuditloggTaskDTO()).thenReturn(auditloggTaskDTO);
        when(auditloggLineMessageHeader.getTeamGcpProjectId()).thenReturn(TEAM_PROJECT_ID);
        when(auditloggLineMessageHeader.getPlaceInPacket()).thenReturn(PLACE_IN_PACKET_INT);

        processor.initializeExchangeVariablesForLogLine(exchange);

        assertEquals(POSTGRESQL, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class));
        assertEquals(auditloggTaskDTO, exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class));
        assertEquals(TEAM_PROJECT_ID, exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class));
        assertEquals(PLACE_IN_PACKET_INT, exchange.getVariable(PLACE_IN_PACKET));
    }

    @Test
    void incrementMetricsForPacket_incrementsHappyPathAndDatabaseSpecificMetrics() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEKNOLOGI, POSTGRESQL);

        processor.incrementMetricsForPacket(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.packet, POSTGRESQL, Metrics.Action.consumed);
    }

    @Test
    void incrementMetricsForLine_incrementsHappyPathAndDatabaseSpecificMetrics() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEKNOLOGI, POSTGRESQL);
        exchange.setVariable(AUDITLOGG_TASK, auditloggTaskDTO);
        when(auditloggTaskDTO.getDbname()).thenReturn(DB_NAME);

        processor.incrementMetricsForLine(exchange);

        verify(metrics).incrementDatabaseSpecificAction(DB_NAME, POSTGRESQL, Metrics.Action.consumed);
    }
}
