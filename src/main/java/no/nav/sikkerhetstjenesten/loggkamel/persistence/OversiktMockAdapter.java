package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class OversiktMockAdapter implements OversiktAdapter {

    @Override
    public OversiktEntity save(OversiktEntity entity) {
        return null;
    }

    @Override
    public OversiktEntity findByDbnameAndTeknologi(String dbname, String teknologi) {
        return OversiktEntity.builder()
                .id(1)
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
