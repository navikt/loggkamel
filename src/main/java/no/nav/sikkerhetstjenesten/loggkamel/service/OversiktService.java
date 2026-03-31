package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.controller.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OversiktService {
    private final OversiktJPAAdapter adapter;

    @Autowired
    public OversiktService(OversiktJPAAdapter adapter) {
        this.adapter = adapter;
    }

    public BackupTaskDTO createOversikt(BackupTaskDTO request) {
        return adapter.createBackupEntity(request);
    }

    public BackupTaskDTO getOversiktByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }
}
