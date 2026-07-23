package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LoggingOptions;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Service
public class NativeLogPacketConsumerProcessor {

    public static final String LOGGING_CLIENT = "LoggingClient";

    private final ObjectMapper objectMapper;
    private final Metrics metrics;

    @Autowired
    public NativeLogPacketConsumerProcessor(ObjectMapper objectMapper, Metrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public void populateFilenameHeader(Exchange exchange) {
        if (exchange.getMessage().getHeader(FILE_NAME, String.class) == null) {
            exchange.getMessage().setHeader(FILE_NAME, exchange.getMessage().getHeader(OBJECT_NAME, String.class));
        }
    }

    public void mapToLogLineList(Exchange exchange) throws Exception {
        List<AuditloggLineMessage> loggLineMessageList = objectMapper.readValue(exchange.getMessage().getBody(String.class), new TypeReference<>() {});
        exchange.getMessage().setBody(loggLineMessageList);
    }

    public void initializeExchangeVariablesForPacket(Exchange exchange) {
        List<AuditloggLineMessage> loggLineMessageList = exchange.getMessage().getBody(List.class);
        AuditloggLineMessageHeader firstHeader = loggLineMessageList.getFirst().getHeader();
        String gcpProjectId = firstHeader.getTeamGcpProjectId();

        exchange.setVariable(LOGGING_CLIENT, LoggingOptions.newBuilder()
                .setProjectId(gcpProjectId)
                .build()
                .getService());
        exchange.setVariable(TEKNOLOGI, firstHeader.getTeknologi());
    }

    public void incrementMetricsForPacket(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.packet, teknologi, Metrics.Action.consumed);
    }

    public void initializeExchangeVariablesForLogLine(Exchange exchange) {
        AuditloggLineMessage loggLineMessage = exchange.getMessage().getBody(AuditloggLineMessage.class);

        exchange.setVariable(TEKNOLOGI, loggLineMessage.getHeader().getTeknologi());
        exchange.setVariable(AUDITLOGG_ARKIV, loggLineMessage.getHeader().getAuditloggArkivResponseDTO());
        exchange.setVariable(TEAM_GCP_PROJECT_ID, loggLineMessage.getHeader().getTeamGcpProjectId());
        exchange.setVariable(PLACE_IN_PACKET, loggLineMessage.getHeader().getPlaceInPacket());
    }

    public void incrementMetricsForLine(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        String dbName = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class).getDbname();
        metrics.incrementDatabaseSpecificAction(dbName, teknologi, Metrics.Action.consumed);
    }
}
