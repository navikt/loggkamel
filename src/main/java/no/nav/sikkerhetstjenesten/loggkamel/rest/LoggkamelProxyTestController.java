package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.boot.conditionals.ConditionalOnDev;
import no.nav.sikkerhetstjenesten.loggkamel.client.LoggkamelProxyClient;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@ConditionalOnDev
@RestController
@RequestMapping("/api/v1/proxy")
public class LoggkamelProxyTestController {

    private static final Logger log = LoggerFactory.getLogger(LoggkamelProxyTestController.class);

    @Autowired
    LoggkamelProxyClient loggkamelProxyClient;

    @GetMapping("test")
    @ResponseStatus(OK)
    public List<AuditloggLineDTO> getHardcodedTestResponse() {
        String databaseName = "AT408T";
        LocalDateTime logStartTime = LocalDateTime.parse("2026-05-30T07:32:15.123");
        LocalDateTime logEndTime = LocalDateTime.parse("2026-07-30T07:32:15.123");

        log.info("Getting hardcoded list of AuditloggLines, testing LoggkamelProxy connection");
        return loggkamelProxyClient.getAuditloggLinesForDatabaseInDateRange(AuditloggRequest.builder()
                .databaseName(databaseName)
                .logStartTime(logStartTime)
                .logEndTime(logEndTime)
                .build());
    }
}
