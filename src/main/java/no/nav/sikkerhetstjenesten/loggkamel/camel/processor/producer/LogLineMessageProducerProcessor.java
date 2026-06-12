package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;

@Service
public class LogLineMessageProducerProcessor {

    private final ObjectMapper objectMapper;
    private final Metrics metrics;

    @Autowired
    public LogLineMessageProducerProcessor(ObjectMapper objectMapper, Metrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public void incrementMetrics(Exchange exchange) {
        String teknologi = exchange.getVariable(TEKNOLOGI, String.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.single, teknologi.toLowerCase(), Metrics.Action.produced);
    }

    public void mapToAuditloggLineMessage(Exchange exchange) throws JsonProcessingException {
        AuditloggLineMessage auditloggLineMessage = AuditloggLineMessage.builder()
                .body(exchange.getMessage().getBody(String.class))
                .header(AuditloggLineMessageHeader.builder()
                        .teknologi(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class))
                        .teamGcpProjectId(exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class))
                        .auditloggArkivResponseDTO(exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class))
                        .build())
                .build();

        exchange.getMessage().setBody(objectMapper.writeValueAsString(auditloggLineMessage));
    }
}

