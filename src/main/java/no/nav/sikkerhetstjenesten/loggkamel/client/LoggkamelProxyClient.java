package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface LoggkamelProxyClient {

    @PostExchange("/api/v1/auditlogg")
    List<DB2AuditloggLineDTO> getDB2AuditloggLinesForDatabaseInDateRange(@RequestBody AuditloggRequest request);
}
