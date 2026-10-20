package no.nav.sikkerhetstjenesten.loggkamel.auth;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AdminNaisteams;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.MissingNaisTeamException;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisTokenIntrospectorTest {

    private static final String ENDPOINT_FIELD = "tokenIntrospectionEndpoint";
    private static final String TOKEN = "user provided token";
    private static final String TOKEN_INTROSPECTION_ENDPOINT_VALUE = "some value for introspection endpoint";
    private static final Map<String, String> EXPECTED_REQUEST_BODY =
            Map.of("identity_provider", "entra_id", "token", TOKEN);
    private static final long ISSUED_AT_EPOCH_SECONDS = 1757570000L;
    private static final long EXPIRES_AT_EPOCH_SECONDS = 1757573600L;
    private static final String AUDIENCE = "some audience";
    private static final List<String> SCOPES = List.of("read", "write");
    private static final String EMAIL = "user@example.test";
    private static final String ADMIN_TEAM = "admin-team";
    private static final String OTHER_ADMIN_TEAM = "other-admin-team";

    @Mock
    private NaisTeamMembershipService membershipService;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    NaisTokenIntrospector naisTokenIntrospector;

    @BeforeEach
    void setUp() {
        naisTokenIntrospector = new NaisTokenIntrospector(restClient, membershipService,
                new AdminNaisteams(List.of(ADMIN_TEAM, OTHER_ADMIN_TEAM)));
        ReflectionTestUtils.setField(naisTokenIntrospector, ENDPOINT_FIELD, TOKEN_INTROSPECTION_ENDPOINT_VALUE);
    }

    @Test
    void missingIntrospectionEndpoint() {
        ReflectionTestUtils.setField(naisTokenIntrospector, ENDPOINT_FIELD, null);
        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
        verifyNoInteractions(membershipService);

        ReflectionTestUtils.setField(naisTokenIntrospector, ENDPOINT_FIELD, "");
        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    @Test
    void authenticationResponseNull() {
        mockIntrospectionResponse(null);

        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    @Test
    void tokenNotValid() {
        mockIntrospectionResponse(Map.of("active", false, "error", "some explanation"));

        assertThrows(BadOpaqueTokenException.class, () -> naisTokenIntrospector.introspect(TOKEN));
        verifyNoInteractions(membershipService);
    }

    @Test
    void tokenIsValid() {
        Map<String, Object> response = Map.of(
                "active", true,
                "azp_name", "some consumer",
                "roles", "access_as_application"
        );
        mockIntrospectionResponse(response);

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        Map<String, Object> expectedClaims = new HashMap<>(response);
        expectedClaims.put(NAISTEAMS_CLAIM, List.of());
        assertEquals(expectedClaims, principal.getAttributes());
        assertIterableEquals(List.of(GRANTED_SERVICE_AUTHORITY), principal.getAuthorities());
        verifyNoInteractions(membershipService);
    }

    @Test
    void numericTimeClaimsAreConvertedToInstant() {
        Map<String, Object> response = Map.of(
                "active", true,
                "iat", Math.toIntExact(ISSUED_AT_EPOCH_SECONDS),
                "exp", EXPIRES_AT_EPOCH_SECONDS,
                "nbf", Math.toIntExact(ISSUED_AT_EPOCH_SECONDS),
                "aud", AUDIENCE,
                "scope", String.join(" ", SCOPES)
        );
        mockIntrospectionResponse(response);

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(Instant.ofEpochSecond(ISSUED_AT_EPOCH_SECONDS), principal.getAttribute("iat"));
        assertEquals(Instant.ofEpochSecond(EXPIRES_AT_EPOCH_SECONDS), principal.getAttribute("exp"));
        assertEquals(Instant.ofEpochSecond(ISSUED_AT_EPOCH_SECONDS), principal.getAttribute("nbf"));
        assertIterableEquals(List.of(AUDIENCE), principal.getAttribute("aud"));
        assertIterableEquals(SCOPES, principal.getAttribute("scope"));
    }

    @Test
    void validTokenWithoutClaims() {
        Map<String, Object> response = Map.of("active", true);
        mockIntrospectionResponse(response);

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(Map.of("active", true, NAISTEAMS_CLAIM, List.of()), principal.getAttributes());
        assertIterableEquals(List.of(GRANTED_SERVICE_AUTHORITY), principal.getAuthorities());
        verifyNoInteractions(membershipService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void unusableUsernameGetsServiceAuthorityAndNoTeams(String email) {
        Map<String, Object> response = new HashMap<>();
        response.put("active", true);
        response.put(EMAIL_CLAIM, email);
        response.put(NAISTEAMS_CLAIM, List.of(ADMIN_TEAM));
        mockIntrospectionResponse(response);

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(List.of(), principal.getAttribute(NAISTEAMS_CLAIM));
        assertIterableEquals(List.of(GRANTED_SERVICE_AUTHORITY), principal.getAuthorities());
        verifyNoInteractions(membershipService);
    }

    @Test
    void userTeamsReplaceUpstreamTeamsAndPreserveEmail() {
        mockIntrospectionResponse(Map.of("active", true, EMAIL_CLAIM, EMAIL,
                NAISTEAMS_CLAIM, List.of(ADMIN_TEAM), "roles", List.of(ADMIN_AUTHORITY)));
        when(membershipService.getAllNaisteamsForEmail(EMAIL)).thenReturn(List.of("team-a", "team-b"));

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(EMAIL, principal.getAttribute(EMAIL_CLAIM));
        assertEquals(List.of("team-a", "team-b"), principal.getAttribute(NAISTEAMS_CLAIM));
        assertIterableEquals(List.of(GRANTED_USER_AUTHORITY), principal.getAuthorities());
        verify(membershipService).getAllNaisteamsForEmail(EMAIL);
    }

    @ParameterizedTest
    @ValueSource(strings = {ADMIN_TEAM, OTHER_ADMIN_TEAM})
    void membershipInAnyConfiguredAdminTeamGrantsUserAndAdminAuthorities(String adminTeam) {
        mockIntrospectionResponse(Map.of("active", true, EMAIL_CLAIM, EMAIL));
        when(membershipService.getAllNaisteamsForEmail(EMAIL)).thenReturn(List.of("team-a", adminTeam));

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(List.of("team-a", adminTeam), principal.getAttribute(NAISTEAMS_CLAIM));
        assertIterableEquals(List.of(GRANTED_USER_AUTHORITY, GRANTED_ADMIN_AUTHORITY), principal.getAuthorities());
    }

    @Test
    void userWithoutTeamsStillGetsUserAuthority() {
        mockIntrospectionResponse(Map.of("active", true, EMAIL_CLAIM, EMAIL));
        when(membershipService.getAllNaisteamsForEmail(EMAIL)).thenReturn(List.of());

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(List.of(), principal.getAttribute(NAISTEAMS_CLAIM));
        assertIterableEquals(List.of(GRANTED_USER_AUTHORITY), principal.getAuthorities());
    }

    @Test
    void missingUsernameCannotUseUpstreamTeamsForAdminAccess() {
        mockIntrospectionResponse(Map.of("active", true, NAISTEAMS_CLAIM, List.of(ADMIN_TEAM)));

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(List.of(), principal.getAttribute(NAISTEAMS_CLAIM));
        assertIterableEquals(List.of(GRANTED_SERVICE_AUTHORITY), principal.getAuthorities());
        verifyNoInteractions(membershipService);
    }

    @Test
    void nonStringUsernameIsRejected() {
        mockIntrospectionResponse(Map.of("active", true, EMAIL_CLAIM, List.of(EMAIL)));

        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
        verifyNoInteractions(membershipService);
    }

    @Test
    void membershipLookupFailureIsWrappedWithOriginalCause() {
        mockIntrospectionResponse(Map.of("active", true, EMAIL_CLAIM, EMAIL));
        RuntimeException failure = new IllegalStateException("Membership lookup unavailable");
        when(membershipService.getAllNaisteamsForEmail(EMAIL)).thenThrow(failure);

        OAuth2IntrospectionException thrown = assertThrows(OAuth2IntrospectionException.class,
                () -> naisTokenIntrospector.introspect(TOKEN));

        assertSame(failure, thrown.getCause());
    }

    @Test
    void missingMembershipResponseIsAnIntrospectionFailure() {
        mockIntrospectionResponse(Map.of("active", true, EMAIL_CLAIM, EMAIL));
        MissingNaisTeamException failure = new MissingNaisTeamException("Missing memberships");
        when(membershipService.getAllNaisteamsForEmail(EMAIL)).thenThrow(failure);

        OAuth2IntrospectionException thrown = assertThrows(OAuth2IntrospectionException.class,
                () -> naisTokenIntrospector.introspect(TOKEN));

        assertSame(failure, thrown.getCause());
    }

    @Test
    void invalidUserTokenDoesNotTriggerMembershipLookup() {
        mockIntrospectionResponse(Map.of("active", false, EMAIL_CLAIM, EMAIL));

        assertThrows(BadOpaqueTokenException.class, () -> naisTokenIntrospector.introspect(TOKEN));
        verifyNoInteractions(membershipService);
    }

    @Test
    void responseWithoutActiveField() {
        mockIntrospectionResponse(Map.of("azp_name", "some consumer"));

        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    @Test
    void responseWithActiveFieldOfWrongType() {
        Map<String, Object> response = new HashMap<>();
        response.put("active", "true");
        response.put("azp_name", "some consumer");
        mockIntrospectionResponse(response);

        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    private void mockIntrospectionResponse(Map<String, Object> response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(TOKEN_INTROSPECTION_ENDPOINT_VALUE)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(EXPECTED_REQUEST_BODY)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(RESPONSE_TYPE)).thenReturn(response);
    }
}
