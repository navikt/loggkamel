package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.boot.conditionals.Cluster;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.NaisServiceGCP.TEAM;
import static no.nav.sikkerhetstjenesten.loggkamel.service.NaisServiceGCP.TEAM_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = {
                NaisServiceGCP.class,
                CacheConfig.class,
                NaisServiceGCPCacheIntegrationTest.TestConfig.class
        },
        properties = "NAIS_CLUSTER_NAME=dev-gcp"
)
class NaisServiceGCPCacheIntegrationTest {

    private static final String NAIS_TEAM = "naisteam";
    private static final String GCP_PROJECT_ID = "gcpProjectId";
    private static final String CLUSTER_NAME = Cluster.currentCluster().clusterName();

    @Autowired
    @Qualifier("naisServiceGCP")
    NaisService service;

    @Autowired
    HttpSyncGraphQlClient naisGraphqlClient;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(naisGraphqlClient);
        clearCache(CacheConfig.NAIS_GCP_PROJECT_BY_TEAM);
    }

    @Test
    void successfulResponseIsCached() {
        stubClientToReturn(new NaisTeamEnvironments(List.of(new GCPProject(CLUSTER_NAME, GCP_PROJECT_ID))));

        String first = service.getCurrentEnvGCPIDForTeam(NAIS_TEAM);
        String second = service.getCurrentEnvGCPIDForTeam(NAIS_TEAM);

        assertEquals(GCP_PROJECT_ID, first);
        assertEquals(GCP_PROJECT_ID, second);
        verify(naisGraphqlClient, times(1)).document(anyString());
    }

    @Test
    void exceptionIsNotCached() {
        GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
        GraphQlClient.RetrieveSpec retrieveSpec = mock(GraphQlClient.RetrieveSpec.class);
        @SuppressWarnings("unchecked")
        Mono<NaisTeamEnvironments> mono = mock(Mono.class);

        when(naisGraphqlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(mono);
        when(mono.block()).thenThrow(new RuntimeException("GraphQL error"));

        assertThrows(NaisDependencyException.class, () -> service.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
        assertThrows(NaisDependencyException.class, () -> service.getCurrentEnvGCPIDForTeam(NAIS_TEAM));

        verify(naisGraphqlClient, times(2)).document(anyString());
    }

    @SuppressWarnings("unchecked")
    private void stubClientToReturn(NaisTeamEnvironments environments) {
        GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
        GraphQlClient.RetrieveSpec retrieveSpec = mock(GraphQlClient.RetrieveSpec.class);
        Mono<NaisTeamEnvironments> mono = mock(Mono.class);

        when(naisGraphqlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(mono);
        when(mono.block()).thenReturn(environments);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        assertNotNull(cache);
        cache.clear();
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        HttpSyncGraphQlClient naisGraphqlClient() {
            return Mockito.mock(HttpSyncGraphQlClient.class);
        }
    }
}
