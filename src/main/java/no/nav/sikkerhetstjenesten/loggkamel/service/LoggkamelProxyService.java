package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.client.LoggkamelProxyAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoggkamelProxyService {

    public static final int PACKET_SIZE = 1000;

    private final LoggkamelProxyAdapter loggkamelProxyAdapter;

    public LoggkamelProxyService(LoggkamelProxyAdapter loggkamelProxyAdapter) {
        this.loggkamelProxyAdapter = loggkamelProxyAdapter;
    }

    public List<DB2AuditloggLineDTO> getDB2AuditloggLinesForDatabaseInDateRange(String databaseName, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return loggkamelProxyAdapter.getDB2AuditloggLinesForDatabaseInDateRange(databaseName, startDateTime, endDateTime, PACKET_SIZE);
    }
}
