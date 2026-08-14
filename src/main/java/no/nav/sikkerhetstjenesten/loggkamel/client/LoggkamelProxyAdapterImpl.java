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
        return proxyClient.getDB2AuditloggLinesForDatabaseInDateRange(AuditloggRequest.builder()
                        .databaseName(databaseName)
                        .logStartTime(startDateTime)
                        .logEndTime(endDateTime)
                        .packetSize(packetSize)
                .build());
    }
}
