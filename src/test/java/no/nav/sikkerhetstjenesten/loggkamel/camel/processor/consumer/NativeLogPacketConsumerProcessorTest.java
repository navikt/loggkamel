package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.Logging;
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
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum.POSTGRESQL;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        exchange.getMessage().setHeader(FILE_NAME, NAME_FROM_CAMEL);

        processor.populateLocalFilenameHeader(exchange);

        assertEquals(NAME_FROM_CAMEL, exchange.getMessage().getHeader(LOG_FILENAME, String.class));
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
        assertEquals(POSTGRESQL, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class));
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
