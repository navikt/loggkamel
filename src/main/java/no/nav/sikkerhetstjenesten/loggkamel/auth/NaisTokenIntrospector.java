package no.nav.sikkerhetstjenesten.loggkamel.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class NaisTokenIntrospector implements OpaqueTokenIntrospector {

    public record EntraAuthenticationResponse(
            boolean active,
            String error,
            List<String> roles
    ) {}

    private static final Logger log = LoggerFactory.getLogger(NaisTokenIntrospector.class);

    static final List<GrantedAuthority> grantedAuthorities = List.of(new SimpleGrantedAuthority("AUTHENTICATED_NAIS_SERVICE"));

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${NAIS_TOKEN_INTROSPECTION_ENDPOINT}")
    private String tokenIntrospectionEndpoint;

    @Autowired
    public NaisTokenIntrospector(
            ObjectMapper objectMapper,
            RestClient restClient
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public @NonNull OAuth2AuthenticatedPrincipal introspect(@NonNull String token) {
        if (tokenIntrospectionEndpoint == null || tokenIntrospectionEndpoint.isBlank()) {
            log.error("Token introspection endpoint environment variable is missing");
            throw new OAuth2IntrospectionException("Token introspection endpoint environment variable is missing");
        }

        Map<String, String> requestBody = Map.of("identity_provider", "entra_id", "token",  token);
        EntraAuthenticationResponse authenticationResponse = restClient.post()
                .uri(tokenIntrospectionEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(EntraAuthenticationResponse.class);

        if (authenticationResponse == null) {
            log.warn("Token introspection endpoint returned an empty body");
            throw new OAuth2IntrospectionException("Token introspection endpoint returned an empty body");
        }

        if (!authenticationResponse.active) {
            log.debug("Invalid token received, cause for invalid token is ${authenticationResponse.error}");
            throw new BadOpaqueTokenException("Invalid token received, cause for invalid token is ${authenticationResponse.error}");
        }

        Map<String, Object> authenticationResponseAsMap = objectMapper.convertValue(authenticationResponse, new TypeReference<>() {});
        return new DefaultOAuth2AuthenticatedPrincipal(authenticationResponseAsMap, grantedAuthorities);
    }
}
