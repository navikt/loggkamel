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

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;

@Service
public class NativeLogPacketProducerProcessor {

    private final ObjectMapper objectMapper;
    private final Metrics metrics;

    @Autowired
    public NativeLogPacketProducerProcessor(ObjectMapper objectMapper, Metrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public void incrementMetrics(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.single, teknologi, Metrics.Action.produced);
    }

    public void mapToAuditloggLineMessageList(Exchange exchange) throws JsonProcessingException {
        List<String> logLines = exchange.getMessage().getBody(List.class);

        List<AuditloggLineMessage> auditloggLineMessageList = logLines.stream()
                .map(logLine -> AuditloggLineMessage.builder()
                        .body(logLine)
                        .header(AuditloggLineMessageHeader.builder()
                                .teknologi(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class))
                                .teamGcpProjectId(exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class))
                                .auditloggArkivResponseDTO(exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class))
                                .build())
                        .build())
                .toList();

        exchange.getMessage().setBody(objectMapper.writeValueAsString(auditloggLineMessageList));
    }
}

