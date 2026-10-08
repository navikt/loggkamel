package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.boot.conditionals.EnvUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile({EnvUtil.LOCAL, EnvUtil.DEV, EnvUtil.TEST})
public class NaisServiceMock implements NaisService {
    @Override
    public String getCurrentEnvGCPIDForTeam(String naisTeam) {
        return "sikkerhetstjenesten-dev-f3ab";
    }

    @Override
    public List<String> getAllNaisteamsForEmail(String email) {
        NaisService.requireEmail(email);
        return List.of("team-a", "team-b", "sikkerhetstjenesten");
    }
}
