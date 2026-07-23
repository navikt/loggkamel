package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static org.springframework.http.HttpStatus.OK;

// TODO: look into input sanitization to avoid sql, log injection
// TODO: declare controller using Spring annotations
// TODO: require authentication for controller, validate token. Likely switch from previous machine-machine to employee auth
//@ProtectedRestController(value = "/api/v1/task", issuer = "azuread", claimMap = {})
@ConditionalOnGCP
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditloggTaskController", description = "Denne brukes til å kontrollere hvilke databaser det skal overføres auditlogger for (tasks)")
public class AuditloggTaskController {

    private static final Logger log = LoggerFactory.getLogger(AuditloggTaskController.class);

    private final OversiktService oversiktService;

    @Autowired
    public AuditloggTaskController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Registrer en ny overførings-task")
    public AuditloggTaskDTO createAuditloggTask(@RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO) {
        log.debug("Creating auditlogg task: {}", auditloggTaskRequestDTO);
        return oversiktService.createAuditloggTask(auditloggTaskRequestDTO);
    }

    @PutMapping()
    @ResponseStatus(OK)
    @Operation(summary = "Oppdater en overførings-task")
    public AuditloggTaskDTO updateAuditloggTask(@RequestBody AuditloggTaskRequestDTO auditloggTaskRequestDTO) {
        log.debug("Updating auditlogg task: {}", auditloggTaskRequestDTO);
        return oversiktService.updateAuditloggTask(auditloggTaskRequestDTO);
    }
}
