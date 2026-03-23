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
    public OversiktEntity findByDbname(String dbname) {
        //TODO: create test data here
        return null;
    }
}
