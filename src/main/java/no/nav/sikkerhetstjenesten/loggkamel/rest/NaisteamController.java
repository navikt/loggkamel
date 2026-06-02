package no.nav.sikkerhetstjenesten.loggkamel.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/naisteam")
@Tag(name = "NaisteamController", description = "Denne kontrolleren skal brukes for å finne audit logg arkiv for naisteam og verifisere hvis naisteam har aktivt nødt for arkiv infrastruktur")
public class NaisteamController {

    private static final Logger log = LoggerFactory.getLogger(NaisteamController.class);

    private final OversiktService oversiktService;

    @Autowired
    public NaisteamController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @GetMapping("auditlogg/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Finne alle audit logg arkiv for gitt nais team")
    public List<AuditloggArkivResponseDTO> getAuditloggArkivByNaisTeam(@PathVariable("naisTeam") String naisTeam) {
        log.info("Getting audit logg arkiv by nais team: {}", naisTeam);
        return oversiktService.getAuditloggArkivByNaisteam(naisTeam);
    }

    @GetMapping("active/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Verifisere hvis en nais team har aktivt nødt for arkiv infrastruktur")
    public Boolean naisteamHasActiveArkivTasks(@PathVariable("naisTeam") String naisTeam) {
        log.info("Confirming active arkiv tasks for naisteam: {}", naisTeam);
        return oversiktService.naisteamHasActiveArkivTasks(naisTeam);
    }

    @GetMapping("active")
    @ResponseStatus(OK)
    @Operation(summary = "Finner alle unik naisteam med aktivt nødt for arkiv infrastruktur")
    public List<String> findAllActiveNaisteam() {
        log.info("Finding all naisteam with active arkiv tasks");
        return oversiktService.findAllNaisteamWithActiveArkivTasks();
    }
}
