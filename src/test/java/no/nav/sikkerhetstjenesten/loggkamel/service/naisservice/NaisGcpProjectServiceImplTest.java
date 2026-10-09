package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import reactor.core.publisher.Mono;

import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisGcpProjectServiceImpl.TEAM;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisGcpProjectServiceImpl.TEAM_NAME;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisGcpProjectServiceImpl.TEAM_ENVIRONMENTS_QUERY;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisGcpProjectServiceImpl.GCPProject;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisGcpProjectServiceImpl.NaisTeamEnvironments;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisGcpProjectServiceImplTest {

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
    NaisGcpProjectServiceImpl naisGcpProjectService;

    @Test
    void getCurrentEnvGCPIDForTeam_graphQlExceptionConvertedToDependencyException() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenThrow(new RuntimeException("GraphQL client error"));

        assertThrows(NaisDependencyException.class, () -> naisGcpProjectService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void getCurrentEnvGCPIDForTeam_noNaisEnvironmentsFoundConvertedToInvalidLogStreamException() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(null);

        assertThrows(InvalidLogStreamException.class, () -> naisGcpProjectService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void getCurrentEnvGCPIDForTeam_noGCPProjectForCurrentClusterConvertedToInvalidLogGroupException() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(naisTeamEnvironments);
        when(naisTeamEnvironments.environments()).thenReturn(java.util.List.of());

        assertThrows(InvalidLogStreamException.class, () -> naisGcpProjectService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void getCurrentEnvGCPIDForTeam_validResponseReturnsGCPProjectID() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(naisTeamEnvironments);
        when(naisTeamEnvironments.environments()).thenReturn(java.util.List.of(gcpProject));
        when(gcpProject.name()).thenReturn("local");
        when(gcpProject.gcpProjectID()).thenReturn(GCP_PROJECT_ID);

        assertEquals(GCP_PROJECT_ID, naisGcpProjectService.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

}