package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class GCPStandardizedLogLineProducerProcessor {

    private static final Logger log = LoggerFactory.getLogger(GCPStandardizedLogLineProducerProcessor.class);

    final static String CLOUD_LOGGING_ENTRY_NAME = "loggkamel-arkiv";

    private final Metrics metrics;
    private final ObjectMapper objectMapper;
    private final GCPLoggingClientFactory gcpLoggingClientFactory;

    @Autowired
    public GCPStandardizedLogLineProducerProcessor(Metrics metrics, ObjectMapper objectMapper, GCPLoggingClientFactory gcpLoggingClientFactory) {
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.gcpLoggingClientFactory = gcpLoggingClientFactory;
    }

    public void incrementMetrics(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.single, teknologi, Metrics.Action.produced);

        String dbName = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class).getDbname();
        metrics.incrementDatabaseSpecificAction(dbName, teknologi, Metrics.Action.produced);
    }

    public void writeToGcpLogging(Exchange exchange) {
        String targetGCPProjectId = exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class);

        try (Logging logging = gcpLoggingClientFactory.create(targetGCPProjectId)) {
            EnrichedAuditlogg enrichedAuditLogg = exchange.getMessage().getBody(EnrichedAuditlogg.class);
            Map<String, Object> logMessageAsMap = objectMapper.convertValue(enrichedAuditLogg, new TypeReference<>() {});
            Payload.JsonPayload logMessageAsJsonPayload = Payload.JsonPayload.of(logMessageAsMap);

            LogEntry entry = LogEntry.newBuilder(logMessageAsJsonPayload)
                    .setSeverity(Severity.INFO)
                    .setLogName(CLOUD_LOGGING_ENTRY_NAME)
                    .setTimestamp(enrichedAuditLogg.getLogTime().toInstant())
                    .setInsertId(DigestUtils.sha256Hex(enrichedAuditLogg.getSqlStatement()))
                    .build();

            //TODO: remove after debugging
            log.info("Log entry being sent to GCP logging: " + objectMapper.writeValueAsString(entry));

            logging.write(Collections.singleton(entry));
        } catch (Exception e) {
            String fileName = exchange.getMessage().getHeader(FILE_NAME, String.class);
            log.warn("Error while writing log entry to GCP Logging for file {}, error message: {}", fileName, e.getMessage());
            throw new GCPDependencyException("Error while writing log entry to GCP Logging for file " + fileName, e);
        }
    }
}

