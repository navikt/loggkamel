package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OversiktService {
    private final OversiktJPAAdapter adapter;

    @Autowired
    public OversiktService(OversiktJPAAdapter adapter) {
        this.adapter = adapter;
    }

    public BackupTaskDTO createBackupTask(BackupTaskDTO request) {
        return adapter.createBackupTask(request);
    }

    public BackupTaskDTO updateBackupTask(BackupTaskDTO request) {
        return adapter.updateBackupTask(request);
    }

    public BackupTaskDTO getBackupTaskByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }

    public List<BackupTaskDTO> getBackupTaskByNaisteam(String naisteam) {
        return adapter.getAllTasksByNaisteam(naisteam);
    }
}
