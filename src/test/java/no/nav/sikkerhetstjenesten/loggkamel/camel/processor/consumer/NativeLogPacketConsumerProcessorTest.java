package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.Logging;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
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
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
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
    private AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    private Metrics metrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NativeLogPacketConsumerProcessor processor;

    @Test
    void populateFilenameHeader_usesObjectNameWhenCamelFileNameIsMissing() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(OBJECT_NAME, NAME_FROM_BUCKET);

        processor.populateFilenameHeader(exchange);

        assertEquals(NAME_FROM_BUCKET, exchange.getIn().getHeader(FILE_NAME, String.class));
    }

    @Test
    void populateFilenameHeader_keepsExistingCamelFileName() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(FILE_NAME, NAME_FROM_CAMEL);
        exchange.getIn().setHeader(OBJECT_NAME, NAME_FROM_BUCKET);

        processor.populateFilenameHeader(exchange);

        assertEquals(NAME_FROM_CAMEL, exchange.getIn().getHeader(FILE_NAME, String.class));
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
        when(auditloggLineMessageHeader.getTeknologi()).thenReturn(TeknologiEnum.POSTGRESQL);
        when(auditloggLineMessageHeader.getAuditloggArkivResponseDTO()).thenReturn(auditloggArkivResponseDTO);
        when(auditloggLineMessageHeader.getTeamGcpProjectId()).thenReturn(TEAM_PROJECT_ID);

        processor.initializeExchangeVariablesForLogLine(exchange);

        assertEquals(TeknologiEnum.POSTGRESQL, exchange.getVariable(TEKNOLOGI, TeknologiEnum.class));
        assertEquals(auditloggArkivResponseDTO, exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class));
        assertEquals(TEAM_PROJECT_ID, exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class));
    }

    @Test
    void incrementMetrics_incrementsHappyPathAndDatabaseSpecificMetrics() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEKNOLOGI, TeknologiEnum.POSTGRESQL);
        exchange.setVariable(AUDITLOGG_ARKIV, auditloggArkivResponseDTO);
        when(auditloggArkivResponseDTO.getDbname()).thenReturn(DB_NAME);

        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.single, TeknologiEnum.POSTGRESQL, Metrics.Action.consumed);
        verify(metrics).incrementDatabaseSpecificAction(DB_NAME, TeknologiEnum.POSTGRESQL, Metrics.Action.consumed);
    }
}

