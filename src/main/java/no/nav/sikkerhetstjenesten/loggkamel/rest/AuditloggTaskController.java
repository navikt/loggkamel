package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisTeamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static org.springframework.http.HttpStatus.OK;

// TODO: look into input sanitization to avoid sql, log injection
@RestController
@RequestMapping("/api/v1/task")
@ConditionalOnGCP
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditloggTaskController", description = "Denne brukes til å kontrollere hvilke databaser det skal overføres auditlogger for (tasks)")
public class AuditloggTaskController {

    private static final Logger log = LoggerFactory.getLogger(AuditloggTaskController.class);

    private final AuditloggTaskService auditloggTaskService;
    private final NaisService naisService;

    @Autowired
    public AuditloggTaskController(AuditloggTaskService auditloggTaskService, NaisService naisService) {
        this.auditloggTaskService = auditloggTaskService;
        this.naisService = naisService;
    }

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

    @GetMapping("mine")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle overførings-tasks for naisteamene den innloggede brukeren er medlem av")
    public List<NaisTeamDTO> getAuditloggTasksForCurrentUser(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        log.info("Getting auditlogg tasks for the naisteams of the user whose token was passed in");
        return auditloggTaskService.getTasksGroupedByNaisteam(findNaisteamsForPrincipal(principal));
    }

    private List<String> findNaisteamsForPrincipal(OAuth2AuthenticatedPrincipal principal) {
        String email = principal == null ? null : principal.getAttribute(NaisService.EMAIL_CLAIM);
        List<String> naisteams = naisService.getAllNaisteamsForEmail(email);
        log.info("Fant {} naisteam for innlogget bruker", naisteams.size());
        return naisteams;
    }
}
