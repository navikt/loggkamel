package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {
        CacheConfig.class,
        NaisTeamMembershipServiceCacheIntegrationTest.TestConfig.class
})
class NaisTeamMembershipServiceCacheIntegrationTest {

    private static final String EMAIL_ADDRESS = "user@example.test";
    private static final String OTHER_EMAIL_ADDRESS = "other@example.test";

    @Autowired
    NaisTeamMembershipService service;

    @Autowired
    HttpSyncGraphQlClient client;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(client);
        var cache = cacheManager.getCache(CacheConfig.NAIS_TEAMS_BY_EMAIL);
        assertNotNull(cache);
        cache.clear();
    }

    @Test
    void successfulMembershipIsCachedByEmail() {
        mockMembership(EMAIL_ADDRESS, Mono.just(membership("team-a")));
        assertEquals(List.of("team-a"), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));

        mockMembership(OTHER_EMAIL_ADDRESS, Mono.just(membership("team-b")));
        assertEquals(List.of("team-b"), service.getAllNaisteamsForEmail(OTHER_EMAIL_ADDRESS));
        assertEquals(List.of("team-a"), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        assertEquals(List.of("team-b"), service.getAllNaisteamsForEmail(OTHER_EMAIL_ADDRESS));

        verify(client, times(2)).document(TEAM_MEMBERSHIPS_FOR_USER_QUERY);
    }

    @Test
    void emptyMembershipIsCached() {
        mockMembership(EMAIL_ADDRESS, Mono.just(membership()));

        assertEquals(List.of(), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        assertEquals(List.of(), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));

        verify(client).document(TEAM_MEMBERSHIPS_FOR_USER_QUERY);
    }

    @Test
    void dependencyFailureIsNotCached() {
        var retrieve = mockMembership(EMAIL_ADDRESS, Mono.error(new IllegalStateException("Lookup failed")));

        assertThrows(NaisDependencyException.class, () -> service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        when(retrieve.toEntity(NaisUserTeamMemberships.class)).thenReturn(Mono.just(membership("team-a")));

        assertEquals(List.of("team-a"), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        assertEquals(List.of("team-a"), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        verify(client, times(2)).document(TEAM_MEMBERSHIPS_FOR_USER_QUERY);
    }

    @Test
    void missingMembershipResponseIsNotCached() {
        var retrieve = mockMembership(EMAIL_ADDRESS, Mono.empty());

        assertThrows(MissingNaisTeamException.class, () -> service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        when(retrieve.toEntity(NaisUserTeamMemberships.class)).thenReturn(Mono.just(membership("team-a")));

        assertEquals(List.of("team-a"), service.getAllNaisteamsForEmail(EMAIL_ADDRESS));
        verify(client, times(2)).document(TEAM_MEMBERSHIPS_FOR_USER_QUERY);
    }

    @Test
    void invalidEmailStillUsesExistingValidation() {
        assertThrows(ForbiddenOperationException.class, () -> service.getAllNaisteamsForEmail(null));
        assertThrows(ForbiddenOperationException.class, () -> service.getAllNaisteamsForEmail(""));
        assertThrows(ForbiddenOperationException.class, () -> service.getAllNaisteamsForEmail(" "));

        verifyNoInteractions(client);
    }

    @Test
    void membershipCacheUsesExistingFifteenMinuteWriteExpiry() {
        var cache = assertInstanceOf(CaffeineCache.class, cacheManager.getCache(CacheConfig.NAIS_TEAMS_BY_EMAIL));
        var expiry = cache.getNativeCache().policy().expireAfterWrite().orElseThrow();

        assertEquals(Duration.ofMinutes(15), expiry.getExpiresAfter());
        assertEquals(200, cache.getNativeCache().policy().eviction().orElseThrow().getMaximum());
    }

    private GraphQlClient.RetrieveSpec mockMembership(String email, Mono<NaisUserTeamMemberships> response) {
        var request = mock(GraphQlClient.RequestSpec.class);
        var retrieve = mock(GraphQlClient.RetrieveSpec.class);
        when(client.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)).thenReturn(request);
        when(request.variable(EMAIL, email)).thenReturn(request);
        when(request.retrieve(USER)).thenReturn(retrieve);
        when(retrieve.toEntity(NaisUserTeamMemberships.class)).thenReturn(response);
        return retrieve;
    }

    private NaisUserTeamMemberships membership(String... teams) {
        return new NaisUserTeamMemberships(new NaisTeamConnection(
                List.of(teams).stream().map(team -> new NaisTeamNode(new NaisTeam(team))).toList()));
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        HttpSyncGraphQlClient naisGraphqlClient() {
            return mock(HttpSyncGraphQlClient.class);
        }

        @Bean
        NaisTeamMembershipService naisTeamMembershipService() {
            return new NaisTeamMembershipServiceImpl();
        }
    }
}
