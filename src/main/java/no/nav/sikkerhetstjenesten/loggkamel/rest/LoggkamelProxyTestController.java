package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.boot.conditionals.ConditionalOnDev;
import no.nav.sikkerhetstjenesten.loggkamel.client.LoggkamelProxyClient;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

//TODO: remove after done testing loggkamelProxy, when it is integrated into DB2 log pull behavior
@ConditionalOnDev
@RestController
@RequestMapping("/api/v1/proxy")
public class LoggkamelProxyTestController {

    private static final Logger log = LoggerFactory.getLogger(LoggkamelProxyTestController.class);

    @Autowired
    LoggkamelProxyClient loggkamelProxyClient;

    @GetMapping("test")
    @ResponseStatus(OK)
    public List<DB2AuditloggLineDTO> getHardcodedTestResponse() {
        String databaseName = "AT408T";
        LocalDateTime logStartTime = LocalDateTime.parse("2026-05-30T07:32:15.123");
        LocalDateTime logEndTime = LocalDateTime.parse("2026-07-30T07:32:15.123");

        log.info("Getting hardcoded list of AuditloggLines, testing LoggkamelProxy connection");
        return loggkamelProxyClient.getDB2AuditloggLinesForDatabaseInDateRange(AuditloggRequest.builder()
                .databaseName(databaseName)
                .logStartTime(logStartTime)
                .logEndTime(logEndTime)
                .packetSize(1000)
                .build());
    }
}
