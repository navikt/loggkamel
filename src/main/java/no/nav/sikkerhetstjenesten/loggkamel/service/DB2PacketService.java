package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.NativeLogPacketProducer.LOG_PACKET_EXTENSION;
import static no.nav.sikkerhetstjenesten.loggkamel.service.LoggkamelProxyService.MAX_DB2_PACKET_SIZE;

@Component
public class DB2PacketService {

    private static final Logger log = LoggerFactory.getLogger(DB2PacketService.class);

    private final PacketPersistenceService packetPersistenceService;

    private final LoggkamelProxyService loggkamelProxyService;

    private final NaisService naisService;

    private final DB2DTOMapper db2DTOMapper;

    @Autowired
    public DB2PacketService(PacketPersistenceService packetPersistenceService, LoggkamelProxyService loggkamelProxyService, NaisService naisService, DB2DTOMapper db2DTOMapper) {
        this.packetPersistenceService = packetPersistenceService;
        this.loggkamelProxyService = loggkamelProxyService;
        this.naisService = naisService;
        this.db2DTOMapper = db2DTOMapper;
    }

    @Async
    public void persistPacketsForTaskAndDateRange(AuditloggTaskDTO auditloggTaskDTO, LocalDate startDate, LocalDate endDate) {
        log.info("Starting persisting packet files for database {}, startDate {}, endDate {}", auditloggTaskDTO.getDbname(), startDate, endDate);
        StopWatch stopWatch = new StopWatch("Fetching, bundling, persisting logs");
        stopWatch.start("Building and persisting packets for database " + auditloggTaskDTO.getDbname());

        String dbName = auditloggTaskDTO.getDbname();
        LocalDateTime packetStartDateTime = startDate.atStartOfDay();
        LocalDateTime packetEndDateTime = endDate.atTime(LocalTime.MAX);

        List<DB2AuditloggLineDTO> currentPacketContents =
                loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, packetStartDateTime, packetEndDateTime);
        persistAuditloggLinesAsPacketsSeparatedByDate(currentPacketContents, auditloggTaskDTO);

        // Breaks if there are more than MAX_DB2_PACKET_SIZE logs with the same timestamp. Given that these timestamps have nanosecond precision, this is judged unlikely
        while(currentPacketContents.size() >= MAX_DB2_PACKET_SIZE) {
            packetStartDateTime = currentPacketContents.getLast().getMetricsTimestamp();

            currentPacketContents = loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, packetStartDateTime, packetEndDateTime);
            persistAuditloggLinesAsPacketsSeparatedByDate(currentPacketContents, auditloggTaskDTO);
        }

        stopWatch.stop();
        log.info("Finished persisting packet files for database {}, startDate {}, endDate {}, runtime in millis: {}",
                auditloggTaskDTO.getDbname(), startDate, endDate, stopWatch.getTotalTimeMillis());
    }

    void persistAuditloggLinesAsPacketsSeparatedByDate(List<DB2AuditloggLineDTO> auditloggLineDTOs, AuditloggTaskDTO auditloggTaskDTO) {
        String gcpId = naisService.getCurrentEnvGCPIDForTeam(auditloggTaskDTO.getNaisteam());

        Map<LocalDate, List<DB2AuditloggLineDTO>> auditloggLinesGroupedByDate =
                auditloggLineDTOs.stream().collect(
                        Collectors.groupingBy(auditloggLineDTO -> auditloggLineDTO.getMetricsTimestamp().toLocalDate()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        for (Map.Entry<LocalDate, List<DB2AuditloggLineDTO>> entry : auditloggLinesGroupedByDate.entrySet()) {
            String packetDate = entry.getKey().format(formatter);
            persistPacketWithGivenDate(entry.getValue(), auditloggTaskDTO, packetDate, gcpId);
        }
    }

    void persistPacketWithGivenDate(List<DB2AuditloggLineDTO> packetAsDB2AuditloggLineDTOs, AuditloggTaskDTO auditloggTaskDTO, String packetDate, String gcpId) {
        List<AuditloggLineMessage> packetAsAuditloggLineMessages = db2DTOMapper.convertDB2DTOsToAuditloggLineMessages(packetAsDB2AuditloggLineDTOs, auditloggTaskDTO, gcpId);

        String currentPacketName = auditloggTaskDTO.getDbname() + "." + packetDate + "." + UUID.randomUUID() + LOG_PACKET_EXTENSION;
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(currentPacketName, packetAsAuditloggLineMessages);
    }
}
