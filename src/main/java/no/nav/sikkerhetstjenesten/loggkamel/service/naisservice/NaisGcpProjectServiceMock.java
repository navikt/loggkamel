package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnLocalOrTest
public class NaisGcpProjectServiceMock implements NaisGcpProjectService {
    @Override
    public String getCurrentEnvGCPIDForTeam(String naisTeam) {
        return "sikkerhetstjenesten-dev-f3ab";
    }
}
