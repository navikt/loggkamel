package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface LoggkamelProxyAdapter {

    List<DB2AuditloggLineDTO> getDB2AuditloggLinesForDatabaseInDateRange(String databaseName, LocalDateTime startDateTime, LocalDateTime endDateTime, int packetSize);
}
