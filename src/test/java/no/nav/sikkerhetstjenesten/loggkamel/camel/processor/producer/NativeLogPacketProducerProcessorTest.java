package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NativeLogPacketProducerProcessorTest {

    private static final String LOG_LINE_1 = "logLine1";
    private static final String LOG_LINE_2 = "logLine2";
    private static final List<String> LOG_LINE_LIST =  List.of(LOG_LINE_1, LOG_LINE_2);
    private static final String TEAM_GCP_PROJECT_ID_VALUE = "team-project-id";
    private static final TeknologiEnum TEKNOLOGI_IN_EXCHANGE = TeknologiEnum.POSTGRESQL;
    private static final String DB_NAME = "dbName";

    private static final String AUDITLOGG_LIST_AS_STRING = "auditloggListAsString";

    @Mock
    private Metrics metrics;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggArkivResponseDTO  auditloggArkivResponseDTO;

    @InjectMocks
    private NativeLogPacketProducerProcessor processor;

    @Test
    void incrementMetrics_incrementsProducedLineMetric() {
        when(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TEKNOLOGI_IN_EXCHANGE);

        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.packet, TEKNOLOGI_IN_EXCHANGE, Metrics.Action.produced);
    }

    @Test
    void mapToAuditloggLineMessageList_setsSerializedEnvelopeBody() throws Exception {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(List.class)).thenReturn(LOG_LINE_LIST);

        when(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class)).thenReturn(TEKNOLOGI_IN_EXCHANGE);
        when(exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class)).thenReturn(TEAM_GCP_PROJECT_ID_VALUE);
        when(exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class)).thenReturn(auditloggArkivResponseDTO);
        when(auditloggArkivResponseDTO.getDbname()).thenReturn(DB_NAME);

        when(objectMapper.writeValueAsString(anyList())).thenReturn(AUDITLOGG_LIST_AS_STRING);

        processor.mapToAuditloggLineMessageList(exchange);

        ArgumentCaptor<List> auditloggListCaptor = ArgumentCaptor.forClass(List.class);
        verify(objectMapper, times(1)).writeValueAsString(auditloggListCaptor.capture());
        List genericCapturedList = auditloggListCaptor.getValue();
        assertInstanceOf(AuditloggLineMessage.class, genericCapturedList.get(0));
        List<AuditloggLineMessage> typedCapturedList = (List<AuditloggLineMessage>) genericCapturedList;

        assertEquals(TEKNOLOGI_IN_EXCHANGE, typedCapturedList.get(0).getHeader().getTeknologi());
        assertEquals(TEAM_GCP_PROJECT_ID_VALUE, typedCapturedList.get(0).getHeader().getTeamGcpProjectId());
        assertEquals(DB_NAME, typedCapturedList.get(0).getHeader().getAuditloggArkivResponseDTO().getDbname());
        assertEquals(LOG_LINE_1, typedCapturedList.get(0).getBody());
        assertEquals(1, typedCapturedList.get(0).getHeader().getPlaceInPacket());

        assertEquals(LOG_LINE_2, typedCapturedList.get(1).getBody());
        assertEquals(2, typedCapturedList.get(1).getHeader().getPlaceInPacket());

        verify(message).setBody(AUDITLOGG_LIST_AS_STRING);
        verify(message).setHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
    }
}

