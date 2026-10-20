package no.nav.sikkerhetstjenesten.loggkamel.auth;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.rest.AdminNaisteams;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import java.util.*;

@Component
@ConditionalOnGCP
@EnableConfigurationProperties(AdminNaisteams.class)
public class NaisTokenIntrospector implements OpaqueTokenIntrospector {

    private static final Logger log = LoggerFactory.getLogger(NaisTokenIntrospector.class);

    public static final String SERVICE_AUTHORITY = "AUTHENTICATED_NAIS_SERVICE";
    static final GrantedAuthority GRANTED_SERVICE_AUTHORITY = new SimpleGrantedAuthority(SERVICE_AUTHORITY);
    public static final String USER_AUTHORITY = "AUTHENTICATED_USER";
    static final GrantedAuthority GRANTED_USER_AUTHORITY = new SimpleGrantedAuthority(USER_AUTHORITY);
    public static final String ADMIN_AUTHORITY = "AUTHENTICATED_ADMIN_USER";
    static final GrantedAuthority GRANTED_ADMIN_AUTHORITY = new SimpleGrantedAuthority(ADMIN_AUTHORITY);

    static final String ACTIVE_FIELD = "active";
    static final String ERROR_FIELD = "error";

    static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private static final TypeDescriptor OBJECT_TYPE = TypeDescriptor.valueOf(Object.class);
    private static final TypeDescriptor INSTANT_TYPE = TypeDescriptor.valueOf(Instant.class);
    private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
    private static final TypeDescriptor STRING_LIST_TYPE = TypeDescriptor.collection(List.class, STRING_TYPE);

    private static Object convertClaim(Object claim, TypeDescriptor targetType) {
        return ClaimConversionService.getSharedInstance().convert(claim, OBJECT_TYPE, targetType);
    }

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

    public static final String EMAIL_CLAIM = "preferred_username";
    public static final String NAISTEAMS_CLAIM = "naisteams";

    private final RestClient restClient;
    private final NaisteamMembershipService naisteamMembershipService;
    private final List<String> adminNaisteams;

    @Value("${NAIS_TOKEN_INTROSPECTION_ENDPOINT:#{''}}")
    private String tokenIntrospectionEndpoint;

    @Autowired
    public NaisTokenIntrospector(
            RestClient restClient,
            NaisteamMembershipService naisteamMembershipService,
            AdminNaisteams adminProperties
    ) {
        this.restClient = restClient;
        this.naisteamMembershipService = naisteamMembershipService;
        this.adminNaisteams = adminProperties.adminTeams();
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

        if (authenticationResponse.containsKey(EMAIL_CLAIM) && authenticationResponse.get(EMAIL_CLAIM) != null && !(authenticationResponse.get(EMAIL_CLAIM) instanceof String)) {
            log.warn("Token introspection response contains the field {} but it is not a String", EMAIL_CLAIM);
            throw new OAuth2IntrospectionException("Token introspection response contains the field " +  EMAIL_CLAIM + " but it is not a String");
        }

        if (!active) {
            log.debug("Invalid token received, cause for invalid token is {}", authenticationResponse.get(ERROR_FIELD));
            throw new BadOpaqueTokenException("Invalid token received, cause for invalid token is " + authenticationResponse.get(ERROR_FIELD));
        }

        Map<String, Object> claims = CLAIM_TYPE_CONVERTER.convert(new HashMap<>(authenticationResponse));
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        List<String> memberOfNaisteams;
        if (!claims.containsKey(EMAIL_CLAIM) || claims.get(EMAIL_CLAIM) == null || ((String) claims.get(EMAIL_CLAIM)).isBlank()) {
            memberOfNaisteams = Collections.emptyList();
            grantedAuthorities.add(GRANTED_SERVICE_AUTHORITY);
        } else {
            try {
                memberOfNaisteams = naisteamMembershipService.getAllNaisteamsForEmail((String) claims.get(EMAIL_CLAIM));
            } catch (Exception e) {
                throw new OAuth2IntrospectionException("Unable to find naisteams for authenticated user", e);
            }
            grantedAuthorities.add(GRANTED_USER_AUTHORITY);
        }

        claims.put(NAISTEAMS_CLAIM, memberOfNaisteams);
        if (memberOfNaisteams.stream().anyMatch(adminNaisteams::contains)) {
            grantedAuthorities.add(GRANTED_ADMIN_AUTHORITY);
        }

        return new DefaultOAuth2AuthenticatedPrincipal(claims, grantedAuthorities);
    }
}
