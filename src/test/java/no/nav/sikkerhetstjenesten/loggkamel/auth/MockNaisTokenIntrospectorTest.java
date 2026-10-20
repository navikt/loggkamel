package no.nav.sikkerhetstjenesten.loggkamel.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.*;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceMock.MOCKED_NAISTEAMS;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.MockNaisTokenIntrospector.MOCK_EMAIL;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.MockNaisTokenIntrospector.MOCK_SUBJECT;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.MockNaisTokenIntrospector.MOCK_TOKEN_LIFETIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockNaisTokenIntrospectorTest {

    private static final String TOKEN = "user provided token";
    private static final String BLANK_TOKEN = "   ";

    private final MockNaisTokenIntrospector mockNaisTokenIntrospector = new MockNaisTokenIntrospector();

    @Test
    void missingTokenIsRejected() {
        assertThrows(BadOpaqueTokenException.class, () -> mockNaisTokenIntrospector.introspect(""));
        assertThrows(BadOpaqueTokenException.class, () -> mockNaisTokenIntrospector.introspect(BLANK_TOKEN));
    }

    @Test
    void anyTokenIsAccepted() {
        OAuth2AuthenticatedPrincipal principal = mockNaisTokenIntrospector.introspect(TOKEN);

        assertEquals(MOCK_EMAIL, principal.getAttribute(NaisTokenIntrospector.EMAIL_CLAIM));
        assertEquals(MOCK_SUBJECT, principal.getAttribute(OAuth2TokenIntrospectionClaimNames.SUB));
        assertEquals(Boolean.TRUE, principal.getAttribute(OAuth2TokenIntrospectionClaimNames.ACTIVE));
        assertIterableEquals(List.of(GRANTED_USER_AUTHORITY, GRANTED_ADMIN_AUTHORITY), principal.getAuthorities());
        assertEquals(MOCKED_NAISTEAMS, principal.getAttribute(NAISTEAMS_CLAIM));
    }

    @Test
    void tokenLifetimeClaimsAreInstants() {
        OAuth2AuthenticatedPrincipal principal = mockNaisTokenIntrospector.introspect(TOKEN);

        Instant issuedAt = principal.getAttribute(OAuth2TokenIntrospectionClaimNames.IAT);
        Instant expiresAt = principal.getAttribute(OAuth2TokenIntrospectionClaimNames.EXP);

        assertEquals(MOCK_TOKEN_LIFETIME, Duration.between(issuedAt, expiresAt));
        assertTrue(expiresAt.isAfter(Instant.now()));
    }
}
