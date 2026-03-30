package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OversiktJPAAdapter implements OversiktAdapter {

    private final OversiktRepository repository;

    @Autowired
    public OversiktJPAAdapter(OversiktRepository repository) {
        this.repository = repository;
    }

    public BackupTask save(BackupTask entity) {
        return repository.save(entity);
    }

    public BackupTask findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return repository.findByDbnameAndTeknologi(dbname, teknologi);
    }


}
