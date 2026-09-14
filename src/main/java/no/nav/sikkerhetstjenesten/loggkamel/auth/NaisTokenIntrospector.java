package no.nav.sikkerhetstjenesten.loggkamel.auth;

import no.nav.boot.conditionals.ConditionalOnGCP;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.core.converter.ClaimConversionService;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnGCP
public class NaisTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger log = LoggerFactory.getLogger(NaisTokenIntrospector.class);

    static final List<GrantedAuthority> grantedAuthorities = List.of(new SimpleGrantedAuthority("AUTHENTICATED_NAIS_SERVICE"));

    static final String ACTIVE_FIELD = "active";
    static final String ERROR_FIELD = "error";

    static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private static final TypeDescriptor OBJECT_TYPE = TypeDescriptor.valueOf(Object.class);
    private static final TypeDescriptor INSTANT_TYPE = TypeDescriptor.valueOf(Instant.class);
    private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
    private static final TypeDescriptor STRING_LIST_TYPE = TypeDescriptor.collection(List.class, STRING_TYPE);

    private static final Converter<Object, ?> INSTANT_CONVERTER = claim -> convertClaim(claim, INSTANT_TYPE);
    private static final Converter<Object, ?> STRING_CONVERTER = claim -> convertClaim(claim, STRING_TYPE);
    private static final Converter<Object, ?> STRING_LIST_CONVERTER = claim -> convertClaim(claim, STRING_LIST_TYPE);
    private static final Converter<Object, ?> SCOPE_CONVERTER = claim -> claim instanceof String scope
            ? Arrays.asList(scope.split(" "))
            : convertClaim(claim, STRING_LIST_TYPE);

    static final ClaimTypeConverter CLAIM_TYPE_CONVERTER = new ClaimTypeConverter(Map.of(
            OAuth2TokenIntrospectionClaimNames.AUD, STRING_LIST_CONVERTER,
            OAuth2TokenIntrospectionClaimNames.CLIENT_ID, STRING_CONVERTER,
            OAuth2TokenIntrospectionClaimNames.EXP, INSTANT_CONVERTER,
            OAuth2TokenIntrospectionClaimNames.IAT, INSTANT_CONVERTER,
            OAuth2TokenIntrospectionClaimNames.ISS, STRING_CONVERTER,
            OAuth2TokenIntrospectionClaimNames.NBF, INSTANT_CONVERTER,
            OAuth2TokenIntrospectionClaimNames.SCOPE, SCOPE_CONVERTER
    ));

    private static Object convertClaim(Object claim, TypeDescriptor targetType) {
        return ClaimConversionService.getSharedInstance().convert(claim, OBJECT_TYPE, targetType);
    }

    private final RestClient restClient;

    @Value("${NAIS_TOKEN_INTROSPECTION_ENDPOINT:#{''}}")
    private String tokenIntrospectionEndpoint;

    @Autowired
    public NaisTokenIntrospector(
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public @NonNull OAuth2AuthenticatedPrincipal introspect(@NonNull String token) {
        if (tokenIntrospectionEndpoint == null || tokenIntrospectionEndpoint.isBlank()) {
            log.error("Token introspection endpoint environment variable is missing");
            throw new OAuth2IntrospectionException("Token introspection endpoint environment variable is missing");
        }

        Map<String, String> requestBody = Map.of("identity_provider", "entra_id", "token",  token);

        Map<String, Object> authenticationResponse = restClient.post()
                .uri(tokenIntrospectionEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(RESPONSE_TYPE);

        if (authenticationResponse == null) {
            log.warn("Token introspection endpoint returned an empty body");
            throw new OAuth2IntrospectionException("Token introspection endpoint returned an empty body");
        }

        if (!(authenticationResponse.get(ACTIVE_FIELD) instanceof Boolean active)) {
            log.warn("Token introspection response is missing the field {} or it is not a boolean", ACTIVE_FIELD);
            throw new OAuth2IntrospectionException("Token introspection response is missing the field " + ACTIVE_FIELD + " or it is not a boolean");
        }

        if (!active) {
            log.debug("Invalid token received, cause for invalid token is {}", authenticationResponse.get(ERROR_FIELD));
            throw new BadOpaqueTokenException("Invalid token received, cause for invalid token is " + authenticationResponse.get(ERROR_FIELD));
        }

        Map<String, Object> claims = CLAIM_TYPE_CONVERTER.convert(new HashMap<>(authenticationResponse));

        return new DefaultOAuth2AuthenticatedPrincipal(claims, grantedAuthorities);
    }
}
