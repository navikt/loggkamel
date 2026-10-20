package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisTeamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.NAISTEAMS_CLAIM;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.USER_AUTHORITY;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/task")
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditloggTaskController", description = "Denne brukes til å kontrollere hvilke databaser det skal overføres auditlogger for (tasks)")
public class AuditloggTaskController {

    private static final Logger log = LoggerFactory.getLogger(AuditloggTaskController.class);

    private final AuditloggTaskService auditloggTaskService;

    @Autowired
    public AuditloggTaskController(AuditloggTaskService auditloggTaskService) {
        this.auditloggTaskService = auditloggTaskService;
    }

    @Secured(USER_AUTHORITY)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Registrer en ny overførings-task")
    public AuditloggTaskDTO createAuditloggTask(
            @RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        log.debug("Creating auditlogg task: {}", auditloggTaskRequestDTO);
        return auditloggTaskService.createAuditloggTask(auditloggTaskRequestDTO, findNaisteamsForPrincipal(principal));
    }

    @Secured(USER_AUTHORITY)
    @PutMapping()
    @ResponseStatus(OK)
    @Operation(summary = "Oppdater en overførings-task")
    public AuditloggTaskDTO updateAuditloggTask(
            @RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        log.debug("Updating auditlogg task: {}", auditloggTaskRequestDTO);
        return auditloggTaskService.updateAuditloggTask(auditloggTaskRequestDTO, findNaisteamsForPrincipal(principal));
    }

    @Secured(USER_AUTHORITY)
    @GetMapping("mine")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle overførings-tasks for naisteamene den innloggede brukeren er medlem av")
    public List<NaisTeamDTO> getAuditloggTasksForCurrentUser(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        log.info("Getting auditlogg tasks for the naisteams of the user whose token was passed in");
        return auditloggTaskService.getTasksGroupedByNaisteam(findNaisteamsForPrincipal(principal));
    }

    private List<String> findNaisteamsForPrincipal(OAuth2AuthenticatedPrincipal principal) {
        List<String> naisteams = principal == null || principal.getAttribute(NAISTEAMS_CLAIM) == null ? Collections.emptyList() : principal.getAttribute(NaisTokenIntrospector.NAISTEAMS_CLAIM);
        log.info("Fant {} naisteam for innlogget bruker", naisteams.size());
        return naisteams;
    }
}
