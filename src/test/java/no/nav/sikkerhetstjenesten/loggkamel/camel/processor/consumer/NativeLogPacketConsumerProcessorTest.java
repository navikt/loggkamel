package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.Logging;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
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

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor.LOGGING_CLIENT;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_TASK;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum.POSTGRESQL;
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
    void populateFilenameHeader_usesObjectNameWhenCamelFileNameIsMissing() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader(OBJECT_NAME, NAME_FROM_BUCKET);

        processor.populateFilenameHeader(exchange);

        assertEquals(NAME_FROM_BUCKET, exchange.getMessage().getHeader(FILE_NAME, String.class));
    }

    @Test
    void populateFilenameHeader_keepsExistingCamelFileName() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader(FILE_NAME, NAME_FROM_CAMEL);
        exchange.getMessage().setHeader(OBJECT_NAME, NAME_FROM_BUCKET);

        processor.populateFilenameHeader(exchange);

        assertEquals(NAME_FROM_CAMEL, exchange.getMessage().getHeader(FILE_NAME, String.class));
    }

    @Test
    void mapToLogLineList_setsBodyAndVariables() throws Exception {
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

        processor.initializeExchangeVariablesForPacket(exchange);

        assertNotNull(exchange.getVariable(LOGGING_CLIENT, Logging.class));
        assertEquals(TEAM_PROJECT_ID, exchange.getVariable(LOGGING_CLIENT, Logging.class).getOptions().getProjectId());
    }

    @Test
    void initializeExchangeVariablesForLogLine_setsVariables() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody(auditloggLineMessage);
        when(auditloggLineMessage.getHeader()).thenReturn(auditloggLineMessageHeader);
        when(auditloggLineMessageHeader.getTeknologi()).thenReturn(POSTGRESQL);
        when(auditloggLineMessageHeader.getAuditloggTaskDTO()).thenReturn(auditloggTaskDTO);
        when(auditloggLineMessageHeader.getTeamGcpProjectId()).thenReturn(TEAM_PROJECT_ID);

        processor.initializeExchangeVariablesForLogLine(exchange);

        assertEquals(POSTGRESQL, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class));
        assertEquals(auditloggTaskDTO, exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class));
        assertEquals(TEAM_PROJECT_ID, exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class));
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

