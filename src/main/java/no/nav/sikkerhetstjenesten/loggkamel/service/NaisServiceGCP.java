package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.boot.conditionals.Cluster;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnGCP
public class NaisServiceGCP implements NaisService {

    private static final Logger log = LoggerFactory.getLogger(NaisServiceGCP.class);

    static final String TEAM_NAME = "teamName";
    static final String TEAM = "team";

    @Autowired
    HttpSyncGraphQlClient naisGraphqlClient;

    @Override
    @Cacheable(cacheNames = CacheConfig.NAIS_GCP_PROJECT_BY_TEAM, key = "#naisTeam", sync = true)
    public String getCurrentEnvGCPIDForTeam(String naisTeam) {
        String query = """
                query Team($teamName: Slug!) {
                     team(slug: $teamName) {
                         environments {
                             gcpProjectID
                             name
                         }
                     }
                 }
                """;

        NaisTeamEnvironments naisTeamEnvironments;
        try {
            naisTeamEnvironments = naisGraphqlClient.document(query)
                    .variable(TEAM_NAME, naisTeam)
                    .retrieve(TEAM)
                    .toEntity(NaisTeamEnvironments.class)
                    .block();
        } catch (Exception e) {
            log.info("Feil ved kall mot nais graphql api for team {}, message: {}", naisTeam, e.getMessage());
            throw new NaisDependencyException("Feil ved kall mot nais graphql api for team " + naisTeam, e);
        }

        if (naisTeamEnvironments == null) {
            throw new InvalidLogGroupException("Fant ingen GCP Projecter for team " + naisTeam + " i nais api response");
        }

        String currentCluster = Cluster.currentCluster().clusterName();
        Optional<GCPProject> currentEnvGCPProject = naisTeamEnvironments.getEnvironments().stream().filter(env -> env.getName().equals(currentCluster)).findFirst();

        if (currentEnvGCPProject.isEmpty()) {
            throw new InvalidLogGroupException("Fant ingen GCP Projecter for team " + naisTeam + " i miljø " + currentCluster);
        }

        return currentEnvGCPProject.get().getGcpProjectID();
    }
}
