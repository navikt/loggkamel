package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggRequest;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;

import java.time.LocalDateTime;
import java.util.List;

public class LoggkamelProxyAdapterImpl implements LoggkamelProxyAdapter {

    private final LoggkamelProxyClient proxyClient;

    public LoggkamelProxyAdapterImpl(LoggkamelProxyClient proxyClient) {
        this.proxyClient = proxyClient;
    }

    @Override
    public List<DB2AuditloggLineDTO> getDB2AuditloggLinesForDatabaseInDateRange(String databaseName, LocalDateTime startDateTime, LocalDateTime endDateTime, int packetSize) {
        List<DB2AuditloggLineDTO> responseWithWhitespaces = proxyClient.getDB2AuditloggLinesForDatabaseInDateRange(AuditloggRequest.builder()
                        .databaseName(databaseName)
                        .logStartTime(startDateTime)
                        .logEndTime(endDateTime)
                        .packetSize(packetSize)
                .build());

        // We trim fields that are char fields in db2, and which therefore might have trailing whitespace
        return responseWithWhitespaces.stream().map(db2AuditloggLineDTO -> DB2AuditloggLineDTO.builder()
                .metricsTimestamp(db2AuditloggLineDTO.getMetricsTimestamp())
                .databaseName(db2AuditloggLineDTO.getDatabaseName().trim())
                .tableName(db2AuditloggLineDTO.getTableName().trim())
                .authId(db2AuditloggLineDTO.getAuthId().trim())
                .sqlQuery(db2AuditloggLineDTO.getSqlQuery())
                .build()).toList();
    }
}
