package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.client.LoggkamelProxyAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoggkamelProxyService {

    public static final int MAX_DB2_PACKET_SIZE = 1000;

    private final LoggkamelProxyAdapter loggkamelProxyAdapter;
    private final NaisService naisService;
    private final ObjectMapper objectMapper;

    public LoggkamelProxyService(LoggkamelProxyAdapter loggkamelProxyAdapter, NaisService naisService, ObjectMapper objectMapper) {
        this.loggkamelProxyAdapter = loggkamelProxyAdapter;
        this.naisService = naisService;
        this.objectMapper = objectMapper;
    }

    public List<DB2AuditloggLineDTO> getDB2AuditloggLinesForDatabaseInDateRange(String databaseName, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return loggkamelProxyAdapter.getDB2AuditloggLinesForDatabaseInDateRange(databaseName, startDateTime, endDateTime, MAX_DB2_PACKET_SIZE);
    }

    public List<AuditloggLineMessage> getDB2AuditloggMessagesForTaskInDateRange(AuditloggTaskDTO auditloggTaskDTO, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<DB2AuditloggLineDTO> db2LogLines = loggkamelProxyAdapter.getDB2AuditloggLinesForDatabaseInDateRange(auditloggTaskDTO.getDbname(), startDateTime, endDateTime, MAX_DB2_PACKET_SIZE);
        String gcpId = naisService.getCurrentEnvGCPIDForTeam(auditloggTaskDTO.getNaisteam());

        List<AuditloggLineMessage> auditloggLineMessagePacket =  new ArrayList<>();
        int i = 1;
        for (DB2AuditloggLineDTO db2LogLine : db2LogLines) {
            AuditloggLineMessage auditloggLineMessage = null;
            try {
                auditloggLineMessage = AuditloggLineMessage.builder()
                        .body(objectMapper.writeValueAsString(db2LogLine))
                        .header(AuditloggLineMessageHeader.builder()
                                .auditloggTaskDTO(auditloggTaskDTO)
                                .teamGcpProjectId(gcpId)
                                .teknologi(TeknologiEnum.DB2)
                                .placeInPacket(i)
                                .build())
                        .build();
            } catch (JsonProcessingException e) {
                throw new InvalidLogLineException("Failed to convert db2LogLine to JSON String", e);
            }

            auditloggLineMessagePacket.add(auditloggLineMessage);

            i++;
        }

        return auditloggLineMessagePacket;
    }
}
