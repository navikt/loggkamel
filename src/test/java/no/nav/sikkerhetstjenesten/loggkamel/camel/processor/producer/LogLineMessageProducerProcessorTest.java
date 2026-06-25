package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogLineMessageProducerProcessorTest {

    private static final String LOG_LINE_BODY = "<something>";
    private static final String TEAM_GCP_PROJECT_ID_VALUE = "team-project-id";
    private static final TeknologiEnum TEKNOLOGI_IN_EXCHANGE = TeknologiEnum.POSTGRESQL;
    private static final String DB_NAME = "dbName";

    @Mock
    private Metrics metrics;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private LogLineMessageProducerProcessor processor;

    @Test
    void incrementMetrics_incrementsProducedSingleMetric() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setVariable(TEKNOLOGI, TEKNOLOGI_IN_EXCHANGE);

        processor.incrementMetrics(exchange);

        verify(metrics).incrementHappyPath(Metrics.Multiplicity.single, TEKNOLOGI_IN_EXCHANGE, Metrics.Action.produced);
    }

    @Test
    void mapToAuditloggLineMessage_setsSerializedEnvelopeBody() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setBody(LOG_LINE_BODY);

        AuditloggArkivResponseDTO auditloggArkiv = AuditloggArkivResponseDTO.builder()
                .naisteam("team")
                .teknologi(TEKNOLOGI_IN_EXCHANGE)
                .dbname(DB_NAME)
                .okonomi(false)
                .arkivlov(false)
                .loggingLeseoperasjoner(true)
                .fiksa(true)
                .loggingEndringer(true)
                .funnetLogger(true)
                .build();

        exchange.setVariable(TEKNOLOGI, TEKNOLOGI_IN_EXCHANGE);
        exchange.setVariable(TEAM_GCP_PROJECT_ID, TEAM_GCP_PROJECT_ID_VALUE);
        exchange.setVariable(AUDITLOGG_ARKIV, auditloggArkiv);

        processor.mapToAuditloggLineMessage(exchange);

        AuditloggLineMessage result = objectMapper.readValue(exchange.getMessage().getBody(String.class), AuditloggLineMessage.class);
        assertEquals(LOG_LINE_BODY, result.getBody());
        assertEquals(TEKNOLOGI_IN_EXCHANGE, result.getHeader().getTeknologi());
        assertEquals(TEAM_GCP_PROJECT_ID_VALUE, result.getHeader().getTeamGcpProjectId());
        assertEquals(DB_NAME, result.getHeader().getAuditloggArkivResponseDTO().getDbname());
    }
}

