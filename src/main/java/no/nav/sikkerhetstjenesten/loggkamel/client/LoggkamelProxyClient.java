package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface LoggkamelProxyClient {

    @PostExchange("/api/v1/auditlogg")
    List<AuditloggLineDTO> getAuditloggLinesForDatabaseInDateRange(@RequestBody AuditloggRequest request);
}
