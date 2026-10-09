package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import jakarta.annotation.PostConstruct;
import no.nav.boot.conditionals.Cluster;
import no.nav.boot.conditionals.EnvUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Profile({EnvUtil.LOCAL, EnvUtil.DEV, EnvUtil.TEST})
public class NaisTeamMembershipServiceMock implements NaisTeamMembershipService {

    private static final Logger log = LoggerFactory.getLogger(NaisTeamMembershipServiceMock.class);

    @PostConstruct
    void rejectNonDevelopmentCluster() {
        Cluster currentCluster = Cluster.currentCluster();
        if (!Set.of(Cluster.LOCAL, Cluster.DEV_GCP, Cluster.TEST).contains(currentCluster)) {
            throw new IllegalStateException(
                    "NaisTeamMembershipServiceMock hardcodes a user's team membership and can give access to unowned databases." +
                            " Should only be run in a lower environment, current environment is: "
                            + currentCluster.clusterName());
        }
        log.warn("Naisteam membership is mocked. Every request for team membership will include sikkerhetstjenesten");
    }

    @Override
    public List<String> getAllNaisteamsForEmail(String email) {
        NaisTeamMembershipService.requireEmail(email);
        return List.of("sikkerhetstjenesten", "other-team", "team-with-no-tasks");
    }
}
