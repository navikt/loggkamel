package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.LoggkamelProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

//TODO: remove after done testing loggkamelProxy, when it is integrated into DB2 log pull behavior
@ConditionalOnDevOrLocal
@RestController
@RequestMapping("/api/v1/proxy")
public class LoggkamelProxyTestController {

    private static final Logger log = LoggerFactory.getLogger(LoggkamelProxyTestController.class);

    @Autowired
    LoggkamelProxyService loggkamelProxyService;

    @Autowired
    AuditloggTaskService auditloggTaskService;

    @GetMapping("db2")
    @ResponseStatus(OK)
    public List<DB2AuditloggLineDTO> getHardcodedDb2Logs() {
        String databaseName = "AT408T";
        LocalDateTime logStartTime = LocalDateTime.parse("2026-05-30T07:32:15.123");
        LocalDateTime logEndTime = LocalDateTime.parse("2026-07-30T07:32:15.123");

        log.info("Getting hardcoded list of DB2AuditloggLineDTOs");

        return loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(databaseName, logStartTime, logEndTime);
    }

    @GetMapping("auditlogg-packet")
    @ResponseStatus(OK)
    public List<AuditloggLineMessage> getHardcodedAuditloggPacket() {
        String databaseName = "AT408T";
        LocalDateTime logStartTime = LocalDateTime.parse("2026-05-30T07:32:15.123");
        LocalDateTime logEndTime = LocalDateTime.parse("2026-07-30T07:32:15.123");
        AuditloggTaskDTO auditloggTaskDto = auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(databaseName, TeknologiEnum.DB2);

        log.info("Getting hardcoded list of AuditloggLines, testing how those are represented as json");

        return loggkamelProxyService.getDB2AuditloggMessagesForTaskInDateRange(auditloggTaskDto, logStartTime, logEndTime);
    }
}
