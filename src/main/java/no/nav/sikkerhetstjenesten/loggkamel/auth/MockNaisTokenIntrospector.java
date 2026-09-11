package no.nav.sikkerhetstjenesten.loggkamel.auth;

import jakarta.annotation.PostConstruct;
import no.nav.boot.conditionals.Cluster;
import no.nav.boot.conditionals.EnvUtil;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@Profile(EnvUtil.LOCAL)
public class MockNaisTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger log = LoggerFactory.getLogger(MockNaisTokenIntrospector.class);

    static final String MOCK_EMAIL = "test-utvikler@nav.no";
    static final String MOCK_SUBJECT = "mocked-local-development-subject";
    static final Duration MOCK_TOKEN_LIFETIME = Duration.ofHours(1);

    static final List<GrantedAuthority> grantedAuthorities =
            List.of(new SimpleGrantedAuthority("MOCK_AUTHENTICATION_LOCAL_DEVELOPMENT_ONLY"));

    @PostConstruct
    void rejectNonLocalCluster() {
        Cluster currentCluster = Cluster.currentCluster();
        if (currentCluster != Cluster.LOCAL) {
            throw new IllegalStateException(
                    "MockNaisTokenIntrospector skips token validation and can only run locally, but the current cluster is "
                            + currentCluster.clusterName());
        }
        log.warn("Token introspection is mocked. Every request carrying a bearer token is authenticated as {}", MOCK_EMAIL);
    }

    @Override
    public @NonNull OAuth2AuthenticatedPrincipal introspect(@NonNull String token) {
        if (token.isBlank()) {
            log.debug("Mocked token introspection rejected a request without a bearer token");
            throw new BadOpaqueTokenException("Missing bearer token");
        }

        Instant issuedAt = Instant.now();
        Map<String, Object> claims = Map.of(
                OAuth2TokenIntrospectionClaimNames.ACTIVE, true,
                OAuth2TokenIntrospectionClaimNames.SUB, MOCK_SUBJECT,
                OAuth2TokenIntrospectionClaimNames.IAT, issuedAt,
                OAuth2TokenIntrospectionClaimNames.EXP, issuedAt.plus(MOCK_TOKEN_LIFETIME),
                NaisService.EMAIL_CLAIM, MOCK_EMAIL
        );

        return new DefaultOAuth2AuthenticatedPrincipal(claims, grantedAuthorities);
    }
}
