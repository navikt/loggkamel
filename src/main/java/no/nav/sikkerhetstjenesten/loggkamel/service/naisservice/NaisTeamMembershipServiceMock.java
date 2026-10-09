package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.boot.conditionals.EnvUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile({EnvUtil.LOCAL, EnvUtil.DEV, EnvUtil.TEST})
public class NaisTeamMembershipServiceMock implements NaisTeamMembershipService {
    @Override
    public List<String> getAllNaisteamsForEmail(String email) {
        NaisTeamMembershipService.requireEmail(email);
        return List.of("sikkerhetstjenesten", "other-team", "team-with-no-tasks");
    }
}
