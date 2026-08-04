package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.boot.conditionals.ConditionalOnDev;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.AuditloggLineDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@ConditionalOnDev
@RestController
@RequestMapping("/api/v1/proxy")
public class LoggkamelProxyTestController {

    private static final Logger log = LoggerFactory.getLogger(LoggkamelProxyTestController.class);

    @GetMapping("test")
    @ResponseStatus(OK)
    public List<AuditloggLineDTO> getHardcodedTestResponse() {


        log.info("Getting auditlogg tasks by nais team: {}", naisTeam);
        return oversiktService.getAuditloggTaskByNaisteam(naisTeam);
    }
}
