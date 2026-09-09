package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
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

    @Autowired
    public AuditloggTaskDevController(AuditloggTaskService auditloggTaskService) {
        this.auditloggTaskService = auditloggTaskService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Registrer en ny overførings-task")
    public AuditloggTaskDTO createAuditloggTask(@RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO) {
        log.debug("Creating auditlogg task: {}", auditloggTaskRequestDTO);
        return auditloggTaskService.createAuditloggTask(auditloggTaskRequestDTO, List.of(auditloggTaskRequestDTO.getNaisteam()));
    }

    @PutMapping()
    @ResponseStatus(OK)
    @Operation(summary = "Oppdater en overførings-task")
    public AuditloggTaskDTO updateAuditloggTask(@RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO) {
        log.debug("Updating auditlogg task: {}", auditloggTaskRequestDTO);
        return auditloggTaskService.updateAuditloggTask(auditloggTaskRequestDTO, naisteamsBypassingMembershipCheck(auditloggTaskRequestDTO));
    }

    // Dev- og lokal-endepunktene har ingen innlogget bruker å slå opp teammedlemskap for.
    // Vi later derfor som om kalleren er medlem av både teamet i forespørselen og teamet
    // som eier en eventuell eksisterende task, slik at tilgangssjekken i servicen ikke slår inn her.
    private List<String> naisteamsBypassingMembershipCheck(AuditloggTaskRequestDTO request) {
        AuditloggTaskDTO existingTask = auditloggTaskService
                .getAuditloggTaskByDbnameAndTeknologi(request.getDbname(), request.getTeknologi());

        return existingTask == null
                ? List.of(request.getNaisteam())
                : List.of(request.getNaisteam(), existingTask.getNaisteam());
    }

}
