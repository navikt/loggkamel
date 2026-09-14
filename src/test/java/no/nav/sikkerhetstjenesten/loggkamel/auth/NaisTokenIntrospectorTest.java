package no.nav.sikkerhetstjenesten.loggkamel.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.RESPONSE_TYPE;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.grantedAuthorities;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    NaisTokenIntrospector naisTokenIntrospector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(naisTokenIntrospector, ENDPOINT_FIELD, TOKEN_INTROSPECTION_ENDPOINT_VALUE);
    }

    @Test
    void missingIntrospectionEndpoint() {
        ReflectionTestUtils.setField(naisTokenIntrospector, ENDPOINT_FIELD, null);
        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));

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

        assertEquals(response, principal.getAttributes());
        assertIterableEquals(grantedAuthorities, principal.getAuthorities());
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
    void validTokenWithoutClaims() {        Map<String, Object> response = Map.of("active", true);
        mockIntrospectionResponse(response);

        OAuth2AuthenticatedPrincipal principal = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(response, principal.getAttributes());
        assertIterableEquals(grantedAuthorities, principal.getAuthorities());
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
