package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Service
public class LogLineMessageConsumerProcessor {

    private final ObjectMapper objectMapper;
    private final Metrics metrics;

    @Autowired
    public LogLineMessageConsumerProcessor(ObjectMapper objectMapper, Metrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public void populateFilenameHeader(Exchange exchange) {
        if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
            exchange.getIn().setHeader(FILE_NAME, exchange.getIn().getHeader(OBJECT_NAME, String.class));
        }
    }

    public void mapToAuditloggLineMessage(Exchange exchange) throws Exception {
        AuditloggLineMessage loggLineMessage = objectMapper.readValue(exchange.getMessage().getBody(String.class), AuditloggLineMessage.class);
        exchange.setVariable(TEKNOLOGI, loggLineMessage.getHeader().getTeknologi());
        exchange.setVariable(AUDITLOGG_ARKIV, loggLineMessage.getHeader().getAuditloggArkivResponseDTO());
        exchange.setVariable(TEAM_GCP_PROJECT_ID, loggLineMessage.getHeader().getTeamGcpProjectId());
        exchange.getMessage().setBody(loggLineMessage, AuditloggLineMessage.class);
    }

    public void incrementMetrics(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.single, teknologi, Metrics.Action.consumed);

        String dbName = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class).getDbname();
        metrics.incrementDatabaseSpecificAction(dbName, teknologi, Metrics.Action.consumed);
    }
}
