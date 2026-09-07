package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/dev/task")
@ConditionalOnDevOrLocal
@Tag(name = "AuditloggTaskDevController", description = "Denne brukes til å kontrollere hvilke databaser det skal overføres auditlogger for (tasks)")
public class AuditloggTaskDevController {

    private static final Logger log = LoggerFactory.getLogger(AuditloggTaskDevController.class);

    private final AuditloggTaskService auditloggTaskService;
    private final NaisService naisService;

    @Autowired
    public AuditloggTaskDevController(AuditloggTaskService auditloggTaskService, NaisService naisService) {
        this.auditloggTaskService = auditloggTaskService;
        this.naisService = naisService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Registrer en ny overførings-task")
    public AuditloggTaskDTO createAuditloggTask(@RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO) {
        log.debug("Creating auditlogg task: {}", auditloggTaskRequestDTO);
        return auditloggTaskService.createAuditloggTask(auditloggTaskRequestDTO);
    }

    @PutMapping()
    @ResponseStatus(OK)
    @Operation(summary = "Oppdater en overførings-task")
    public AuditloggTaskDTO updateAuditloggTask(@RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO) {
        log.debug("Updating auditlogg task: {}", auditloggTaskRequestDTO);
        return auditloggTaskService.updateAuditloggTask(auditloggTaskRequestDTO);
    }

    @GetMapping("naisteams")
    @Operation(summary = "Finner naisteamene til en e-postadresse")
    public List<String> getNaisteamsForEmail(@RequestParam String email) {
        return naisService.getAllNaisteamsForEmail(email);
    }
}
