package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Synchronicity;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogPacketException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.ORIGINAL_FILE_PATH;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.apache.camel.component.file.FileConstants.FILE_ABSOLUTE_PATH;

@Service
public class NativeLogPacketConsumerProcessor {

    public static final String LOGGING_CLIENT = "LoggingClient";
    public static final String PENDING_LOG_ENTRIES = "PendingLogEntries";

    private static final Logger log = LoggerFactory.getLogger(NativeLogPacketConsumerProcessor.class);

    private final ObjectMapper objectMapper;
    private final Metrics metrics;

    @Autowired
    public NativeLogPacketConsumerProcessor(ObjectMapper objectMapper, Metrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public void populateLocalFilenameHeader(Exchange exchange) {
        exchange.setProperty(
                ORIGINAL_FILE_PATH,
                exchange.getMessage().getHeader(FILE_ABSOLUTE_PATH, String.class)
        );
        exchange.getMessage().setHeader(LOG_FILENAME, exchange.getMessage().getHeader(FILE_NAME, String.class));
    }

    public void populateGCPFilenameHeader(Exchange exchange) {
        exchange.getMessage().setHeader(LOG_FILENAME, exchange.getMessage().getHeader(OBJECT_NAME, String.class));
    }

    public void mapToLogLineList(Exchange exchange) throws Exception {
        List<AuditloggLineMessage> loggLineMessageList = objectMapper.readValue(exchange.getMessage().getBody(String.class), new TypeReference<>() {});
        exchange.getMessage().setBody(loggLineMessageList);
    }

    public void initializeExchangeVariablesForPacket(Exchange exchange) {
        List<AuditloggLineMessage> loggLineMessageList = exchange.getMessage().getBody(List.class);
        AuditloggLineMessageHeader firstHeader = loggLineMessageList.getFirst().getHeader();
        String gcpProjectId = firstHeader.getTeamGcpProjectId();

        Logging logging = LoggingOptions.newBuilder()
                .setProjectId(gcpProjectId)
                .build()
                .getService();
        // Async writes (the default) report failures only to stderr, so a lost log line would go unnoticed
        logging.setWriteSynchronicity(Synchronicity.SYNC);

        exchange.setVariable(LOGGING_CLIENT, logging);
        exchange.setVariable(PENDING_LOG_ENTRIES, new ArrayList<LogEntry>());
        exchange.setVariable(TEKNOLOGI, firstHeader.getTeknologi());
    }

    public void writePendingLogEntries(Exchange exchange) {
        Logging logging = exchange.getVariable(LOGGING_CLIENT, Logging.class);
        List<LogEntry> pendingLogEntries = exchange.getVariable(PENDING_LOG_ENTRIES, List.class);
        String filename = exchange.getMessage().getHeader(LOG_FILENAME, String.class);

        if (logging == null || pendingLogEntries == null) {
            throw new InvalidLogPacketException("Logging client or pending log entries missing for packet " + filename);
        }
        if (pendingLogEntries.isEmpty()) {
            return;
        }

        try {
            logging.write(pendingLogEntries);
        } catch (Exception e) {
            log.warn("Error while writing {} log entries to GCP Logging for packet {}, error message: {}", pendingLogEntries.size(), filename, e.getMessage());
            throw new GCPDependencyException("Error while writing log entries to GCP Logging for packet " + filename, e);
        }
    }

    public void closeLoggingClient(Exchange exchange) {
        Logging logging = exchange.getVariable(LOGGING_CLIENT, Logging.class);
        if (logging == null) {
            return;
        }
        try {
            logging.close();
        } catch (Exception e) {
            log.warn("Failed to close logging client for packet {}", exchange.getMessage().getHeader(LOG_FILENAME), e);
        }
    }

    public void incrementBackoutFailureMetric(Exchange exchange) {
        metrics.incrementBackoutFailure(Metrics.Multiplicity.packet);
    }

    public void incrementMetricsForPacket(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        metrics.incrementHappyPath(Metrics.Multiplicity.packet, teknologi, Metrics.Action.consumed);
    }

    public void initializeExchangeVariablesForLogLine(Exchange exchange) {
        AuditloggLineMessage loggLineMessage = exchange.getMessage().getBody(AuditloggLineMessage.class);

        exchange.setVariable(TEKNOLOGI, loggLineMessage.getHeader().getTeknologi());
        exchange.setVariable(AUDITLOGG_TASK, loggLineMessage.getHeader().getAuditloggTaskDTO());
        exchange.setVariable(TEAM_GCP_PROJECT_ID, loggLineMessage.getHeader().getTeamGcpProjectId());
        exchange.setVariable(PLACE_IN_PACKET, loggLineMessage.getHeader().getPlaceInPacket());
    }

    public void incrementMetricsForLine(Exchange exchange) {
        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);
        String dbName = exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class).getDbname();
        metrics.incrementDatabaseSpecificAction(dbName, teknologi, Metrics.Action.consumed);
    }
}
