package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggLineDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDateTime;
import java.util.List;

public interface LoggkamelProxyClient {

    @GetExchange("/api/v1/auditlogg")
    List<AuditloggLineDTO> getAuditloggLinesForDatabaseInDateRange(@RequestParam("databaseName") String databaseName,
                                                                   @RequestParam("logStartTime") LocalDateTime logStartTime,
                                                                   @RequestParam("logEndTime") LocalDateTime logEndTime);
}
