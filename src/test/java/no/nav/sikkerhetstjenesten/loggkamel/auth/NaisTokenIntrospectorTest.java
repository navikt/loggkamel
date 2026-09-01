package no.nav.sikkerhetstjenesten.loggkamel.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.grantedAuthorities;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisTokenIntrospectorTest {

    private static final String TOKEN = "user provided token";
    private static final String TOKEN_INTROSPECTION_ENDPOINT_VALUE = "some value for introspection endpoint";

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    NaisTokenIntrospector naisTokenIntrospector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(naisTokenIntrospector, "tokenIntrospectionEndpoint", TOKEN_INTROSPECTION_ENDPOINT_VALUE);
    }

    @Test
    void missingIntrospectionEndpoint() {
        ReflectionTestUtils.setField(naisTokenIntrospector, "tokenIntrospectionEndpoint", null);
        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));

        ReflectionTestUtils.setField(naisTokenIntrospector, "tokenIntrospectionEndpoint", "");
        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    @Test
    void authenticationResponseNull() {
        mockRequestToResponseSpec();
        when(responseSpec.body(NaisTokenIntrospector.EntraAuthenticationResponse.class)).thenReturn(null);

        assertThrows(OAuth2IntrospectionException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    private void mockRequestToResponseSpec() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(TOKEN_INTROSPECTION_ENDPOINT_VALUE)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyMap())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void tokenNotValid() {
        mockRequestToResponseSpec();
        NaisTokenIntrospector.EntraAuthenticationResponse entraAuthenticationResponse = new NaisTokenIntrospector.EntraAuthenticationResponse(false, "some explanation", null);
        when(responseSpec.body(NaisTokenIntrospector.EntraAuthenticationResponse.class)).thenReturn(entraAuthenticationResponse);

        assertThrows(BadOpaqueTokenException.class, () -> naisTokenIntrospector.introspect(TOKEN));
    }

    @Test
    void tokenIsValid() {
        mockRequestToResponseSpec();
        NaisTokenIntrospector.EntraAuthenticationResponse entraAuthenticationResponse = new NaisTokenIntrospector.EntraAuthenticationResponse(true, null, List.of("some", "roles"));
        when(responseSpec.body(NaisTokenIntrospector.EntraAuthenticationResponse.class)).thenReturn(entraAuthenticationResponse);

        Map<String, Object> mapFromResponse = Map.of("key1", "value1", "key2", "value2");
        when(objectMapper.convertValue(eq(entraAuthenticationResponse), any(TypeReference.class))).thenReturn(mapFromResponse);

        OAuth2AuthenticatedPrincipal tokenInspectionResponse = naisTokenIntrospector.introspect(TOKEN);

        assertEquals(mapFromResponse, tokenInspectionResponse.getAttributes());
        assertIterableEquals(grantedAuthorities, tokenInspectionResponse.getAuthorities());
    }

}