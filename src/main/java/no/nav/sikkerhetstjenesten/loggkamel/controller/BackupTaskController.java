package no.nav.sikkerhetstjenesten.loggkamel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.security.token.support.spring.UnprotectedRestController;
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

//TODO: make protected, deploy in all GCP environments
// TODO: set up interceptor to handle exceptions, map to meaningful http status codes
// TODO: look into input sanitization to avoid sql, log injection
@UnprotectedRestController("/api/v1")
@ConditionalOnDevOrLocal
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "BackupTaskController", description = "Denne kontrolleren skal brukes for å kontrollere backup tasks")
public class BackupTaskController {

    private static final Logger log = LoggerFactory.getLogger(BackupTaskController.class);

    private final OversiktService oversiktService;

    @Autowired
    public BackupTaskController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @PostMapping(path = "backupTask", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    @Operation(summary = "Generer en backup task for gitt dbname og teknologi")
    public BackupTaskDTO createBackupTask(@RequestBody BackupTaskDTO backupTask) {
        log.debug("Creating backup task: {}", backupTask);
        return oversiktService.createBackupTask(backupTask);
    }

    @PutMapping("backupTask")
    @ResponseStatus(OK)
    @Operation(summary = "Oppdatere en backup task for gitt dbname og teknologi")
    public BackupTaskDTO updateBackupTask(@RequestBody BackupTaskDTO backupTask) {
        log.debug("Updating backup task: {}", backupTask);
        return oversiktService.updateBackupTask(backupTask);
    }

    @GetMapping("backupTask/{naisTeam}")
    @ResponseStatus(OK)
    @Operation(summary = "Finne alle backup tasks for gitt nais team")
    public List<BackupTaskDTO> getBackupTasksByNaisTeam(@PathVariable("naisTeam") String naisTeam) {
        log.debug("Getting backup tasks by nais team: {}", naisTeam);
        return oversiktService.getBackupTaskByNaisteam(naisTeam);
    }
}
