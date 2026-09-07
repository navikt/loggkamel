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

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.TEAM;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.TEAM_NAME;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.USER;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.EMAIL;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.TEAM_ENVIRONMENTS_QUERY;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.TEAM_MEMBERSHIPS_FOR_USER_QUERY;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.GCPProject;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.NaisTeam;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.NaisTeamConnection;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.NaisTeamEnvironments;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.NaisTeamNode;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisServiceGCP.NaisUserTeamMemberships;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisServiceGCPTest {

    private static final String NAIS_TEAM = "naisteam";
    private static final String GCP_PROJECT_ID = "gcpProjectId";
    private static final String USER_EMAIL = "user@nav.no";

    @Mock
    GraphQlClient.RequestSpec requestSpec;

    @Mock
    GraphQlClient.RetrieveSpec retrieveSpec;

    @Mock
    Mono<NaisTeamEnvironments> naisTeamEnvironmentsMono;

    @Mock
    Mono<NaisUserTeamMemberships> naisUserTeamMembershipsMono;

    @Mock
    NaisTeamEnvironments naisTeamEnvironments;

    @Mock
    GCPProject gcpProject;

    @Mock
    HttpSyncGraphQlClient naisGraphqlClient;

    @InjectMocks
    NaisServiceGCP naisServiceGCP;

    @Test
    void getCurrentEnvGCPIDForTeam_graphQlExceptionConvertedToDependencyException() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenThrow(new RuntimeException("GraphQL client error"));

        assertThrows(NaisDependencyException.class, () -> naisServiceGCP.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void getCurrentEnvGCPIDForTeam_noNaisEnvironmentsFoundConvertedToInvalidLogStreamException() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(null);

        assertThrows(InvalidLogStreamException.class, () -> naisServiceGCP.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void getCurrentEnvGCPIDForTeam_noGCPProjectForCurrentClusterConvertedToInvalidLogGroupException() {
        when(naisGraphqlClient.document(TEAM_ENVIRONMENTS_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(TEAM_NAME, NAIS_TEAM)).thenReturn(requestSpec);
        when(requestSpec.retrieve(TEAM)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisTeamEnvironments.class)).thenReturn(naisTeamEnvironmentsMono);
        when(naisTeamEnvironmentsMono.block()).thenReturn(naisTeamEnvironments);
        when(naisTeamEnvironments.environments()).thenReturn(java.util.List.of());

        assertThrows(InvalidLogStreamException.class, () -> naisServiceGCP.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
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

        assertEquals(GCP_PROJECT_ID, naisServiceGCP.getCurrentEnvGCPIDForTeam(NAIS_TEAM));
    }

    @Test
    void getAllNaisteamsForEmail_returnsNaisteamsForEmail() {
        when(naisGraphqlClient.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(EMAIL, USER_EMAIL)).thenReturn(requestSpec);
        when(requestSpec.retrieve(USER)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisUserTeamMemberships.class)).thenReturn(naisUserTeamMembershipsMono);
        when(naisUserTeamMembershipsMono.block()).thenReturn(new NaisUserTeamMemberships(
                new NaisTeamConnection(List.of(new NaisTeamNode(new NaisTeam(NAIS_TEAM))))
        ));

        assertEquals(List.of(NAIS_TEAM), naisServiceGCP.getAllNaisteamsForEmail(USER_EMAIL));
    }

    @Test
    void getAllNaisteamsForEmail_missingTeamMembershipsThrowsRuntimeException() {
        when(naisGraphqlClient.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(EMAIL, USER_EMAIL)).thenReturn(requestSpec);
        when(requestSpec.retrieve(USER)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisUserTeamMemberships.class)).thenReturn(naisUserTeamMembershipsMono);
        when(naisUserTeamMembershipsMono.block()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> naisServiceGCP.getAllNaisteamsForEmail(USER_EMAIL));
    }

}