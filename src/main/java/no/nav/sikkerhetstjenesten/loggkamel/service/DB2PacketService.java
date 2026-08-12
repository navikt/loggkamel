package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.packet.PacketPersistenceService;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.NativeLogPacketProducer.LOG_PACKET_EXTENSION;
import static no.nav.sikkerhetstjenesten.loggkamel.service.LoggkamelProxyService.MAX_DB2_PACKET_SIZE;

@Component
public class DB2PacketService {

    private static final Logger log = LoggerFactory.getLogger(DB2PacketService.class);

    //TODO: move from autowired to constructor injection
    @Autowired
    private PacketPersistenceService packetPersistenceService;

    @Autowired
    private LoggkamelProxyService loggkamelProxyService;

    @Autowired
    NaisService naisService;

    @Autowired
    ObjectMapper objectMapper;

    @Async
    public void persistPacketsForTaskAndDateRange(AuditloggTaskDTO auditloggTaskDTO, LocalDate startDate, LocalDate endDate) {
        log.info("Starting persisting packet files for database {}, startDate {}, endDate {}", auditloggTaskDTO, startDate, endDate);
        StopWatch stopWatch = new StopWatch("Fetching and packeting logs");
        stopWatch.start("Building and persisting packets for database " + auditloggTaskDTO.getDbname());

        while (!startDate.isAfter(endDate)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String packetDate = startDate.format(formatter);
            String dbName = auditloggTaskDTO.getDbname();

            LocalDateTime packetStartDateTime = startDate.atStartOfDay();
            LocalDateTime packetEndDateTime = startDate.atTime(LocalTime.MAX);

            List<DB2AuditloggLineDTO> currentPacketContents = loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, packetStartDateTime, packetEndDateTime);
            persistCurrentPacket(currentPacketContents, auditloggTaskDTO, packetDate);

            // Breaks if there are more than MAX_DB2_PACKET_SIZE logs with the same timestamp. Given that these timestamps have nanosecond precision, this is judged unlikely
            while(currentPacketContents.size() >= MAX_DB2_PACKET_SIZE) {
                packetStartDateTime = currentPacketContents.getLast().getMetricsTimestamp();

                currentPacketContents = loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, packetStartDateTime, packetEndDateTime);
                persistCurrentPacket(currentPacketContents, auditloggTaskDTO, packetDate);
            }

            startDate = startDate.plusDays(1);
        }

        stopWatch.stop();
        log.info("Finished persisting packet files for database {}, startDate {}, endDate {}, runtime in millis: {}", auditloggTaskDTO, startDate, endDate, stopWatch.getTotalTimeMillis());
    }

    void persistCurrentPacket(List<DB2AuditloggLineDTO> packetAsDB2AuditloggLineDTOs, AuditloggTaskDTO auditloggTaskDTO, String packetDate) {
        String gcpId = naisService.getCurrentEnvGCPIDForTeam(auditloggTaskDTO.getNaisteam());

        List<AuditloggLineMessage> packetAsAuditloggLineMessages =  new ArrayList<>();
        int i = 1;
        for (DB2AuditloggLineDTO db2LogLine : packetAsDB2AuditloggLineDTOs) {
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

            packetAsAuditloggLineMessages.add(auditloggLineMessage);

            i++;
        }

        String currentPacketName = auditloggTaskDTO.getDbname() + "." + packetDate + "." + UUID.randomUUID() + LOG_PACKET_EXTENSION;
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(currentPacketName, packetAsAuditloggLineMessages);
    }
}
