package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class OversiktMockAdapter implements OversiktAdapter {

    @Override
    public Oversikt save(Oversikt entity) {
        return null;
    }

    @Override
    public Oversikt findByDbnameAndTeknologi(String dbname, String teknologi) {
        return Oversikt.builder()
                .id(1L)
                .naisteam("NAIS Team")
                .teknologi(TeknologiEnum.valueOf(teknologi))
                .dbname(dbname)
                .okonomi(true)
                .arkiv(true)
                .personvern(true)
                .fiksa(true)
                .build();
    }
}
