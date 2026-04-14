package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogGroupException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import reactor.core.publisher.Mono;

import static no.nav.sikkerhetstjenesten.loggkamel.service.NaisService.TEAM;
import static no.nav.sikkerhetstjenesten.loggkamel.service.NaisService.TEAM_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisServiceTest {

    private static final String NAIS_TEAM = "naisteam";
    private static final String GCP_PROJECT_ID = "gcpProjectId";

    @Mock
    GraphQlClient.RequestSpec requestSpec;

    @Mock
    GraphQlClient.RetrieveSpec retrieveSpec;

    @Mock
    Mono<NaisTeamEnvironments> naisTeamEnvironmentsMono;

    @Mock
    NaisTeamEnvironments naisTeamEnvironments;

    @Mock
    GCPProject gcpProject;

    @Mock
    HttpSyncGraphQlClient naisGraphqlClient;

    @InjectMocks
    NaisService naisService;

    @Test
    void graphQlExceptionConvertedToDependencyException() {
        when(naisGraphqlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenThrow(new RuntimeException("GraphQL client error"));

        assertThrows(NaisDependencyException.class, () -> naisService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void noNaisEnvironmentsFoundConvertedToInvalidLogGroupException() {
        when(naisGraphqlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(null);

        assertThrows(InvalidLogGroupException.class, () -> naisService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void noGCPProjectForCurrentClusterConvertedToInvalidLogGroupException() {
        when(naisGraphqlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(naisTeamEnvironments);
        when(naisTeamEnvironments.getEnvironments()).thenReturn(java.util.List.of());

        assertThrows(InvalidLogGroupException.class, () -> naisService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void validResponseReturnsGCPProjectID() {
        when(naisGraphqlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(naisTeamEnvironments);
        when(naisTeamEnvironments.getEnvironments()).thenReturn(java.util.List.of(gcpProject));
        when(gcpProject.getName()).thenReturn("local");
        when(gcpProject.getGcpProjectID()).thenReturn(GCP_PROJECT_ID);

        assertEquals(GCP_PROJECT_ID, naisService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

}