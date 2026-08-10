package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;

import java.time.LocalDateTime;
import java.util.List;

public class LoggkamelProxyAdapterMock implements LoggkamelProxyAdapter {

    @Override
    public List<DB2AuditloggLineDTO> getDB2AuditloggLinesForDatabaseInDateRange(String databaseName, LocalDateTime startDateTime, LocalDateTime endDateTime, int packetSize) {
        return List.of(DB2AuditloggLineDTO.builder()
                        .metricsTimestamp(LocalDateTime.now())
                        .databaseName("MOCK_DATABASE_NAME")
                        .authId("MOCK_AUTH_ID")
                        .tableName("MOCK_TABLE_NAME")
                        .sqlQuery("MOCK SQL QUERY")
                .build());
    }
}
