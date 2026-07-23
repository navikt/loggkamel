package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/naisteam")
@Tag(name = "NaisteamController", description = "Denne brukes til å finne alle overførings-tasks for et naisteam og sjekke om et naisteam har noen overførings-tasks.")
public class NaisteamController {

    private static final Logger log = LoggerFactory.getLogger(NaisteamController.class);

    private final OversiktService oversiktService;

    @Autowired
    public NaisteamController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @GetMapping("auditlogg/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle overførings-tasks for et gitt naisteam")
    public List<AuditloggTaskDTO> getAuditloggTasksByNaisTeam(@PathVariable("naisTeam") String naisTeam) {
        log.info("Getting auditlogg tasks by nais team: {}", naisTeam);
        return oversiktService.getAuditloggTaskByNaisteam(naisTeam);
    }

    @GetMapping("active/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Sjekker om naisteamet har noen aktive overførings-tasks")
    public Boolean naisteamHasActiveAuditloggTasks(@PathVariable("naisTeam") String naisTeam) {
        log.info("Confirming active auditlogg tasks for naisteam: {}", naisTeam);
        return oversiktService.naisteamHasActiveAuditloggTasks(naisTeam);
    }

    @GetMapping("active")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle unike naisteam med aktive overførings-tasks")
    public List<String> findAllActiveNaisteam() {
        log.info("Finding all naisteams with active auditlogg tasks");
        return oversiktService.findAllNaisteamWithActiveAuditloggTasks();
    }
}
