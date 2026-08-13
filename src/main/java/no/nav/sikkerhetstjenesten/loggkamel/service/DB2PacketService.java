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
import java.util.Set;
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
    public void fetchLogsWithinDateRangeAndPersistAsPackets(AuditloggTaskDTO auditloggTaskDTO, LocalDate logPullStartDate, LocalDate logPullEndDate) {
        log.info("Starting persisting log packet files for DB2 database {}, startDate {}, endDate {}", auditloggTaskDTO.getDbname(), logPullStartDate, logPullEndDate);
        StopWatch stopWatch = new StopWatch("Fetching, bundling, persisting logs");
        stopWatch.start("Building and persisting packets for database " + auditloggTaskDTO.getDbname());

        String dbName = auditloggTaskDTO.getDbname();
        LocalDateTime logsStartDateTime = logPullStartDate.atStartOfDay();
        LocalDateTime logsEndDateTime = logPullEndDate.atTime(LocalTime.MAX);

        List<DB2AuditloggLineDTO> logsPulledForDateRange =
                loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, logsStartDateTime, logsEndDateTime);
        int pulledPacketSize = logsPulledForDateRange.size();
        persistAuditloggLinesAsPacketsSeparatedByDate(logsPulledForDateRange, auditloggTaskDTO);

        // If the set of logs we got is of the maximum packet size, there may be more logs. Update start date and pull again
        // Breaks if there are more than MAX_DB2_PACKET_SIZE logs with the same timestamp. Given that these timestamps have nanosecond precision, this is judged unlikely
        while(pulledPacketSize >= MAX_DB2_PACKET_SIZE) {
            logsStartDateTime = logsPulledForDateRange.getLast().getMetricsTimestamp();

            LocalDateTime finalLogsStartDateTime = logsStartDateTime;
            Set<DB2AuditloggLineDTO> logsAlreadyPersisted = logsPulledForDateRange.stream()
                    .filter(db2AuditloggLineDTO -> finalLogsStartDateTime.isEqual(db2AuditloggLineDTO.getMetricsTimestamp())).collect(Collectors.toSet());

            logsPulledForDateRange = loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, logsStartDateTime, logsEndDateTime);
            pulledPacketSize = logsPulledForDateRange.size();

            // Want to avoid double-persisting logs, so filter logs out from the new response which were included in the prior response
            logsPulledForDateRange = logsPulledForDateRange.stream().filter(db2AuditloggLineDTO -> !logsAlreadyPersisted.contains(db2AuditloggLineDTO)).toList();

            persistAuditloggLinesAsPacketsSeparatedByDate(logsPulledForDateRange, auditloggTaskDTO);
        }

        stopWatch.stop();
        log.info("Finished persisting log packet files for DB2 database {}, startDate {}, endDate {}, runtime in millis: {}",
                auditloggTaskDTO.getDbname(), logPullStartDate, logPullEndDate, stopWatch.getTotalTimeMillis());
    }

    void persistAuditloggLinesAsPacketsSeparatedByDate(List<DB2AuditloggLineDTO> auditloggLineDTOs, AuditloggTaskDTO auditloggTaskDTO) {
        String gcpId = naisService.getCurrentEnvGCPIDForTeam(auditloggTaskDTO.getNaisteam());

        Map<LocalDate, List<DB2AuditloggLineDTO>> auditloggLinesGroupedByDate =
                auditloggLineDTOs.stream().collect(
                        Collectors.groupingBy(auditloggLineDTO -> auditloggLineDTO.getMetricsTimestamp().toLocalDate()));

        for (Map.Entry<LocalDate, List<DB2AuditloggLineDTO>> entry : auditloggLinesGroupedByDate.entrySet()) {
            persistPacketWithGivenDate(entry.getValue(), entry.getKey(), auditloggTaskDTO, gcpId);
        }
    }

    void persistPacketWithGivenDate(List<DB2AuditloggLineDTO> packetAsDB2AuditloggLineDTOs, LocalDate packetDate, AuditloggTaskDTO auditloggTaskDTO, String gcpId) {
        List<AuditloggLineMessage> packetAsAuditloggLineMessages = db2DTOMapper.convertDB2DTOsToAuditloggLineMessages(packetAsDB2AuditloggLineDTOs, auditloggTaskDTO, gcpId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String packetDateAsString = packetDate.format(formatter);

        String currentPacketName = auditloggTaskDTO.getDbname() + "." + packetDateAsString + "." + UUID.randomUUID() + LOG_PACKET_EXTENSION;
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(currentPacketName, packetAsAuditloggLineMessages);
    }
}
