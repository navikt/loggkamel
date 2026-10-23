package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.NAISTEAMS_CLAIM;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.USER_AUTHORITY;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/naisteam")
@Tag(name = "NaisteamController", description = "Denne brukes til å finne alle overførings-tasks for et naisteam og sjekke om et naisteam har noen overførings-tasks.")
public class NaisteamController {

    private static final Logger log = LoggerFactory.getLogger(NaisteamController.class);

    private final AuditloggTaskService auditloggTaskService;

    @Autowired
    public NaisteamController(AuditloggTaskService auditloggTaskService) {
        this.auditloggTaskService = auditloggTaskService;
    }

    @GetMapping("auditlogg/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle overførings-tasks for et gitt naisteam")
    public List<AuditloggTaskDTO> getAuditloggTasksByNaisTeam(@PathVariable("naisTeam") String naisTeam) {
        log.info("Getting auditlogg tasks by nais team: {}", naisTeam);
        return auditloggTaskService.getAuditloggTaskByNaisteam(naisTeam);
    }

    @GetMapping("auditlogg/by-team")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle overførings-tasks, samlet på naisteam")
    public List<NaisteamDTO> getAuditloggTasksGroupedByNaisTeam() {
        return auditloggTaskService.getAllTasksGroupedByNaisteam();
    }

    @GetMapping("active/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Sjekker om naisteamet har noen aktive overførings-tasks")
    public Boolean naisteamHasActiveAuditloggTasks(@PathVariable("naisTeam") String naisTeam) {
        log.info("Confirming active auditlogg tasks for naisteam: {}", naisTeam);
        return auditloggTaskService.naisteamHasActiveAuditloggTasks(naisTeam);
    }

    @GetMapping("active")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle unike naisteam med aktive overførings-tasks")
    public List<String> findAllActiveNaisteam() {
        log.info("Finding all naisteams with active auditlogg tasks");
        return auditloggTaskService.findAllNaisteamWithActiveAuditloggTasks();
    }

    @Secured(USER_AUTHORITY)
    @GetMapping("mine")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle naisteam den innloggede brukeren er medlem av")
    @SecurityRequirement(name = "bearerAuth")
    public List<String> findNaisteamsForCurrentUser(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        List<String> naisteams = principal == null || principal.getAttribute(NAISTEAMS_CLAIM) == null ? Collections.emptyList() : principal.getAttribute(NaisTokenIntrospector.NAISTEAMS_CLAIM);
        log.info("Fant {} naisteam for innlogget bruker", naisteams.size());
        return naisteams;
    }
}
