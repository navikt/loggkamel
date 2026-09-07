package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnLocalOrTest
public class NaisServiceMock implements NaisService {
    @Override
    public String getCurrentEnvGCPIDForTeam(String naisTeam) {
        return "sikkerhetstjenesten-dev-f3ab";
    }

    @Override
    public List<String> getAllNaisteamsForEmail(String email) {
        return List.of("team-a", "team-b", "sikkerhetstjenesten");
    }
}
