package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.Oversikt;
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

    public Oversikt createOversikt(String naisteam, String teknologi, String dbname,
                                         boolean okonomi, boolean arkiv, boolean personvern, boolean fiksa) {
        //TODO: Add handling for if someone attempts an invalid teknologi field, or if dbname already exists

        Oversikt newEntity = Oversikt.builder()
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

    public Oversikt getOversiktByDbnameAndTeknologi(String dbname, String teknologi) {
        return adapter.findByDbnameAndTeknologi(dbname, teknologi);
    }
}
