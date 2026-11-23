package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.ADMIN_AUTHORITY;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/auditlogg-backfills")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AuditloggBackfillController", description = "Administrerer backfill- og cold-storage-oppgaver")
public class AuditloggBackfillController {

    private final AuditloggTaskService auditloggTaskService;

    public AuditloggBackfillController(AuditloggTaskService auditloggTaskService) {
        this.auditloggTaskService = auditloggTaskService;
    }

    @Secured(ADMIN_AUTHORITY)
    @GetMapping("/requested")
    @Operation(summary = "Henter alle backfill- og cold-storage-oppgaver som venter på behandling")
    public List<NaisteamDTO> getRequestedTasks() {
        return auditloggTaskService.findRequestedBackfillTasksGroupedByNaisteam();
    }

    @Secured(ADMIN_AUTHORITY)
    @PostMapping("/{teknologi}/{dbName}/complete")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Markerer en backfill- eller cold-storage-oppgave som fullført")
    public void completeRequestedTask(
            @PathVariable("teknologi") TeknologiEnum teknoloji,
            @PathVariable("dbName") String dbName
    ) {
        auditloggTaskService.completeRequestedTask(dbName, teknoloji);
    }

}
