package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.boot.conditionals.Cluster;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@ConditionalOnGCP
public class NaisServiceGCP implements NaisService {

    private static final Logger log = LoggerFactory.getLogger(NaisServiceGCP.class);

    static final String TEAM_NAME = "teamName";
    static final String TEAM = "team";
    static final String USER = "user";
    static final String EMAIL = "email";
    static final String TEAM_ENVIRONMENTS_QUERY = """
            query Team($teamName: Slug!) {
                 team(slug: $teamName) {
                     environments {
                         gcpProjectID
                         name
                     }
                 }
             }
            """;
    static final String TEAM_MEMBERSHIPS_FOR_USER_QUERY = """
            query TeamMembershipsForUser($email: String!) {
              user(email: $email) {
                teams {
                  nodes {
                    team {
                      slug
                    }
                  }
                }
              }
            }
            """;

    public record GCPProject(String name, String gcpProjectID) {}

    public record NaisTeamEnvironments(List<GCPProject> environments) {}

    public record NaisUserTeamMemberships(NaisTeamConnection teams) {}

    public record NaisTeamConnection(List<NaisTeamNode> nodes) {}

    public record NaisTeamNode(NaisTeam team) {}

    public record NaisTeam(String slug) {}

    @Autowired
    private HttpSyncGraphQlClient naisGraphqlClient;

    @Override
    @Cacheable(cacheNames = CacheConfig.NAIS_GCP_PROJECT_BY_TEAM, key = "#naisTeam", sync = true)
    public String getCurrentEnvGCPIDForTeam(String naisTeam) {
        NaisTeamEnvironments naisTeamEnvironments;
        try {
            naisTeamEnvironments = naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)
                    .variable(TEAM_NAME, naisTeam)
                    .retrieve(TEAM)
                    .toEntity(NaisTeamEnvironments.class)
                    .block();
        } catch (Exception e) {
            log.warn("Feil ved kall mot nais graphql api for team {}, message: {}", naisTeam, e.getMessage());
            throw new NaisDependencyException("Feil ved kall mot nais graphql api for team " + naisTeam, e);
        }

        if (naisTeamEnvironments == null) {
            throw new InvalidLogStreamException("Fant ingen GCP Projecter for team " + naisTeam + " i nais api response");
        }

        String currentCluster = Cluster.currentCluster().clusterName();
        Optional<GCPProject> currentEnvGCPProject = naisTeamEnvironments.environments().stream().filter(env -> env.name().equals(currentCluster)).findFirst();

        if (currentEnvGCPProject.isEmpty()) {
            throw new InvalidLogStreamException("Fant ingen GCP Projecter for team " + naisTeam + " i miljø " + currentCluster);
        }

        return currentEnvGCPProject.get().gcpProjectID();
    }

    @Override
    public List<String> getAllNaisteamsForEmail(String email) {
        NaisService.requireEmail(email);

        NaisUserTeamMemberships memberships;
        try {
            memberships = naisGraphqlClient.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)
                    .variable(EMAIL, "carlos.sierra@nav.no") //TESTING: remove before merging
//                    .variable(EMAIL, email)
                    .retrieve(USER)
                    .toEntity(NaisUserTeamMemberships.class)
                    .block();
        } catch (Exception e) {
            log.warn("Feil ved kall mot nais graphql api for e-post, message: {}", e.getMessage());
            throw new NaisDependencyException("Feil ved kall mot nais graphql api for e-post", e);
        }

        if (memberships == null || memberships.teams() == null || memberships.teams().nodes() == null) {
            throw new MissingNaisTeamException("Mangler teammedlemskap i nais api response for e-post");
        }

        return memberships.teams().nodes().stream()
                .map(node -> Objects.requireNonNull(node, "Teammedlemskap kan ikke være null").team())
                .map(team -> Objects.requireNonNull(team, "Naisteam kan ikke være null").slug())
                .map(slug -> Objects.requireNonNull(slug, "Naisteam-slug kan ikke være null"))
                .toList();
    }
}
