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

    public Oversikt save(Oversikt entity) {
        return repository.save(entity);
    }

    public Oversikt findByDbnameAndTeknologi(String dbname, String teknologi) {
        return repository.findByDbnameAndTeknologi(dbname, TeknologiEnum.valueOf(teknologi));
    }


}
