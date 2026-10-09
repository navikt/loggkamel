package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.boot.conditionals.EnvUtil;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Profile({EnvUtil.PROD})
public class NaisTeamMembershipServiceImpl implements NaisTeamMembershipService {

    private static final Logger log = LoggerFactory.getLogger(NaisTeamMembershipServiceImpl.class);

    static final String USER = "user";
    static final String EMAIL = "email";
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

    public record NaisUserTeamMemberships(NaisTeamConnection teams) {}

    public record NaisTeamConnection(List<NaisTeamNode> nodes) {}

    public record NaisTeamNode(NaisTeam team) {}

    public record NaisTeam(String slug) {}

    @Autowired
    private HttpSyncGraphQlClient naisGraphqlClient;

    @Override
    public List<String> getAllNaisteamsForEmail(String email) {
        NaisTeamMembershipService.requireEmail(email);

        NaisUserTeamMemberships memberships;
        try {
            memberships = naisGraphqlClient.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)
                    .variable(EMAIL, email)
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
