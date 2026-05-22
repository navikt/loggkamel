package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.security.token.support.spring.UnprotectedRestController;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
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

@UnprotectedRestController(value = "/api/v1/dev/arkiv")
@ConditionalOnDevOrLocal
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditloggArkivDevController", description = "Denne kontrolleren skal brukes for å kontrollere audit logg arkiv")
public class AuditloggArkivDevController {

    private static final Logger log = LoggerFactory.getLogger(AuditloggArkivDevController.class);

    private final OversiktService oversiktService;

    @Autowired
    public AuditloggArkivDevController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Registrerer en DB for Audit Logg Arkivering")
    public AuditloggArkivResponseDTO createAuditloggArkiv(@RequestBody AuditloggArkivRequestDTO auditloggArkivRequestDTO) {
        log.debug("Creating audit logg arkiv: {}", auditloggArkivRequestDTO);
        return oversiktService.createAuditloggArkiv(auditloggArkivRequestDTO);
    }

    @PutMapping()
    @ResponseStatus(OK)
    @Operation(summary = "Oppdatere Audit Logg Arkivering for en gitt DB navn og teknologi")
    public AuditloggArkivResponseDTO updateAuditloggArkiv(@RequestBody AuditloggArkivRequestDTO auditloggArkivRequestDTO) {
        log.debug("Updating audit logg arkiv: {}", auditloggArkivRequestDTO);
        return oversiktService.updateAuditloggArkiv(auditloggArkivRequestDTO);
    }
}

