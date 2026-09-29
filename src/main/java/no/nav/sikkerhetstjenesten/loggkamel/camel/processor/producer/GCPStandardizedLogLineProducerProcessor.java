package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogPacketException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.util.GCPTimestampProvider;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.apache.camel.Exchange;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor.PENDING_LOG_ENTRIES;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.*;

@Service
public class GCPStandardizedLogLineProducerProcessor {

    final static String CLOUD_LOGGING_ENTRY_NAME = "loggkamel-arkiv";

    private final Metrics metrics;
    private final ObjectMapper objectMapper;
    private final GCPTimestampProvider gcpTimestampProvider;

    @Autowired
    public GCPStandardizedLogLineProducerProcessor(Metrics metrics, ObjectMapper objectMapper,  GCPTimestampProvider gcpTimestampProvider) {
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.gcpTimestampProvider = gcpTimestampProvider;
    }

    public void incrementMetrics(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.line, teknologi, Metrics.Action.produced);

        String dbName = exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class).getDbname();
        metrics.incrementDatabaseSpecificAction(dbName, teknologi, Metrics.Action.produced);
    }

    public void queueLogEntry(Exchange exchange) {
        List<LogEntry> pendingLogEntries = exchange.getVariable(PENDING_LOG_ENTRIES, List.class);

        EnrichedAuditlogg enrichedAuditLogg = exchange.getMessage().getBody(EnrichedAuditlogg.class);

        if (pendingLogEntries == null || enrichedAuditLogg == null) {
            throw new InvalidLogPacketException("Log line upload attempted with missing pending log entries or log body");
        }

        Map<String, Object> logMessageAsMap = objectMapper.convertValue(enrichedAuditLogg, new TypeReference<>() {});
        Payload.JsonPayload logMessageAsJsonPayload = Payload.JsonPayload.of(logMessageAsMap);

        LogEntry entry = LogEntry.newBuilder(logMessageAsJsonPayload)
                .setSeverity(Severity.INFO)
                .setLogName(CLOUD_LOGGING_ENTRY_NAME)
                .setTimestamp(gcpTimestampProvider.getTimestampFromLogTime(enrichedAuditLogg.getLogTime()))
                .setInsertId(DigestUtils.sha256Hex(enrichedAuditLogg.getSqlStatement() + enrichedAuditLogg.getSqlParameters()))
                .build();

        pendingLogEntries.add(entry);
    }
}
