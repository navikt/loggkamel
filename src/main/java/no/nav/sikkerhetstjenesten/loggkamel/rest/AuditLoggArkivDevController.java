package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.boot.conditionals.ConditionalOnProd;
import no.nav.security.token.support.spring.UnprotectedRestController;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static org.springframework.http.HttpStatus.OK;

@UnprotectedRestController(value = "/api/v1/dev")
@ConditionalOnDevOrLocal
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditLoggArkivDevController", description = "Denne kontrolleren skal brukes for å kontrollere audit logg arkiv")
public class AuditLoggArkivDevController {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggArkivDevController.class);

    private final OversiktService oversiktService;

    @Autowired
    public AuditLoggArkivDevController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @PostMapping(path = "arkiv", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Registrerer en DB for Audit Log Arkivering")
    public AuditLoggArkivResponseDTO createAuditLoggArkiv(@RequestBody AuditLoggArkivRequestDTO auditLoggArkivRequestDTO) {
        log.debug("Creating audit logg arkiv: {}", auditLoggArkivRequestDTO);
        return oversiktService.createAuditLoggArkiv(auditLoggArkivRequestDTO);
    }

    @PutMapping("arkiv")
    @ResponseStatus(OK)
    @Operation(summary = "Oppdatere Audit Log Arkivering for en gitt DB navn og teknologi")
    public AuditLoggArkivResponseDTO updateAuditLoggArkiv(@RequestBody AuditLoggArkivRequestDTO auditLoggArkivRequestDTO) {
        log.debug("Updating audit logg arkiv: {}", auditLoggArkivRequestDTO);
        return oversiktService.updateAuditLoggArkiv(auditLoggArkivRequestDTO);
    }

    @GetMapping("arkiv/search/naisteam/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Finne alle audit logg arkiv for gitt nais team")
    public List<AuditLoggArkivResponseDTO> getAuditLoggArkivByNaisTeam(@PathVariable("naisTeam") String naisTeam) {
        log.debug("Getting audit logg arkiv by nais team: {}", naisTeam);
        return oversiktService.getAuditLoggArkivByNaisteam(naisTeam);
    }
}

