package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.BackupTask;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OversiktService {
    private final OversiktAdapter adapter;

    @Autowired
    public OversiktService(OversiktAdapter adapter) {
        this.adapter = adapter;
    }

    public BackupTask createOversikt(String naisteam, String teknologi, String dbname,
                                     boolean okonomi, boolean arkiv, boolean personvern, boolean fiksa) {
        //TODO: Add handling for if someone attempts an invalid teknologi field, or if dbname already exists

        BackupTask newEntity = BackupTask.builder()
                .naisteam(naisteam)
                .teknologi(TeknologiEnum.valueOf(teknologi))
                .dbname(dbname)
                .okonomi(okonomi)
                .arkiv(arkiv)
                .personvern(personvern)
                .fiksa(fiksa)
                .build();

        return adapter.save(newEntity);
    }

    public BackupTask getOversiktByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }
}
