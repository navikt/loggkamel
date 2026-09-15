package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2PacketService;
import no.nav.sikkerhetstjenesten.loggkamel.service.PullRerunService;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/pull")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "PullRerunController", description = "Denne brukes til å administrere og manuelt kjøre på nytt feilede loggpuller")
@EnableConfigurationProperties(PullRerunAdminProperties.class)
public class PullRerunController {

    private static final Logger log = LoggerFactory.getLogger(PullRerunController.class);

    private final PullRerunService pullRerunService;
    private final AuditloggTaskService auditloggTaskService;
    private final DB2PacketService db2PacketService;
    private final NaisService naisService;
    private final List<String> adminNaisteams;

    @Autowired
    public PullRerunController(PullRerunService pullRerunService, AuditloggTaskService auditloggTaskService, DB2PacketService db2PacketService, NaisService naisService,
                                PullRerunAdminProperties adminProperties) {
        this.pullRerunService = pullRerunService;
        this.auditloggTaskService = auditloggTaskService;
        this.db2PacketService = db2PacketService;
        this.naisService = naisService;
        this.adminNaisteams = adminProperties.adminTeams();
    }

    @GetMapping("rerun-required")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle loggpuller som har feilet og krever manuell rekjøring")
    public List<PullRerunRequiredDTO> getUnresolvedReruns(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        requireAdminTeamMembership(principal);
        return pullRerunService.findAllUnresolvedReruns();
    }

    @PostMapping("resolve/{id}")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Markerer en feilet loggpull som løst manuelt, uten å kjøre den på nytt")
    public void resolveFailedPull(@PathVariable Long id, @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        requireAdminTeamMembership(principal);

        pullRerunService.findUnresolvedRerunById(id);
        pullRerunService.markRerunResolved(id);
    }

    @PostMapping("rerun/{id}")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Kjører på nytt en feilet loggpull, og markerer den som løst hvis rekjøringen lykkes")
    public void rerunFailedPull(@PathVariable Long id, @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        requireAdminTeamMembership(principal);

        PullRerunRequiredDTO rerun = pullRerunService.findUnresolvedRerunById(id);
        requireSupportedTeknologi(rerun.getTeknologi());

        AuditloggTaskDTO auditloggTask = auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(rerun.getDbname(), rerun.getTeknologi());

        log.info("Kjører på nytt feilet loggpull med id {} for dbname {}", id, rerun.getDbname());
        try {
            db2PacketService.fetchLogsWithinDateRangeAndPersistAsPackets(auditloggTask, rerun.getPullStartDate(), rerun.getPullEndDate());
        } catch (RuntimeException exception) {
            pullRerunService.registerFailedPull(rerun.getDbname(), rerun.getTeknologi(), rerun.getPullStartDate(), rerun.getPullEndDate(), exception);
            throw exception;
        }
        pullRerunService.markRerunResolved(id);
    }

    private void requireSupportedTeknologi(TeknologiEnum teknologi) {
        if (teknologi != TeknologiEnum.DB2) {
            throw new UnsupportedPullTeknologiException("Manuell rekjøring støttes foreløpig kun for DB2, fikk " + teknologi);
        }
    }

    private void requireAdminTeamMembership(OAuth2AuthenticatedPrincipal principal) {
        String email = principal == null ? null : principal.getAttribute(NaisService.EMAIL_CLAIM);
        List<String> naisteams = naisService.getAllNaisteamsForEmail(email);
        if (naisteams.stream().noneMatch(adminNaisteams::contains)) {
            log.warn("Bruker uten medlemskap i et av naisteamene {} forsøkte å administrere loggpuller", adminNaisteams);
            throw new ForbiddenOperationException("Denne operasjonen krever medlemskap i et av naisteamene " + adminNaisteams);
        }
    }
}
