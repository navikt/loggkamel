package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnLocalOrTest
public class NaisServiceMock implements NaisService {
    @Override
    public String getCurrentEnvGCPIDForTeam(String naisTeam) {
        return "sikkerhetstjenesten-dev-f3ab";
    }
}
