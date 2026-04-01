package no.nav.sikkerhetstjenesten.loggkamel.controller;

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

import java.util.List;

//TODO: make protected, deploy in all GCP environments
// TODO: set up interceptor to handle exceptions, map to meaningful http status codes
// TODO: look into input sanitization to avoid sql, log injection
@UnprotectedRestController("/api/v1")
@ConditionalOnDevOrLocal
public class BackupTaskController {

    private static final Logger log = LoggerFactory.getLogger(BackupTaskController.class);

    private final OversiktService oversiktService;

    @Autowired
    public BackupTaskController(OversiktService oversiktService) {
        this.oversiktService = oversiktService;
    }

    @PostMapping(path = "backupTask", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BackupTaskDTO createBackupTask(@RequestBody BackupTaskDTO backupTask) {
        log.debug("Creating backup task: {}", backupTask);
        return oversiktService.createBackupTask(backupTask);
    }

    @PutMapping("backupTask")
    public BackupTaskDTO updateBackupTask(@RequestBody BackupTaskDTO backupTask) {
        log.debug("Updating backup task: {}", backupTask);
        return oversiktService.updateBackupTask(backupTask);
    }

    @GetMapping("backupTask/{naisTeam}")
    public List<BackupTaskDTO> getBackupTasksByNaisTeam(@PathVariable("naisTeam") String naisTeam) {
        log.debug("Getting backup tasks by nais team: {}", naisTeam);
        return oversiktService.getBackupTaskByNaisteam(naisTeam);
    }
}
