package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.NaisDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.EMAIL;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.NaisTeam;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.NaisTeamConnection;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.NaisTeamNode;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.NaisUserTeamMemberships;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.TEAM_MEMBERSHIPS_FOR_USER_QUERY;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisTeamMembershipServiceImpl.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisTeamMembershipServiceImplTest {

    private static final String NAIS_TEAM = "naisteam";
    private static final String USER_EMAIL = "user@nav.no";

    @Mock
    GraphQlClient.RequestSpec requestSpec;

    @Mock
    GraphQlClient.RetrieveSpec retrieveSpec;

    @Mock
    Mono<NaisUserTeamMemberships> naisUserTeamMembershipsMono;

    @Mock
    HttpSyncGraphQlClient naisGraphqlClient;

    @InjectMocks
    NaisTeamMembershipServiceImpl naisTeamMembershipService;

    @Test
    void getAllNaisteamsForEmail_graphQlExceptionConvertedToDependencyException() {
        when(naisGraphqlClient.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(EMAIL, USER_EMAIL)).thenReturn(requestSpec);
        when(requestSpec.retrieve(USER)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisUserTeamMemberships.class)).thenReturn(naisUserTeamMembershipsMono);
        when(naisUserTeamMembershipsMono.block()).thenThrow(new RuntimeException("GraphQL client error"));

        assertThrows(NaisDependencyException.class, () -> naisTeamMembershipService.getAllNaisteamsForEmail(USER_EMAIL));
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

        assertEquals(List.of(NAIS_TEAM), naisTeamMembershipService.getAllNaisteamsForEmail(USER_EMAIL));
    }

    @Test
    void getAllNaisteamsForEmail_missingTeamMembershipsThrowsMissingNaisTeamException() {
        when(naisGraphqlClient.document(TEAM_MEMBERSHIPS_FOR_USER_QUERY)).thenReturn(requestSpec);
        when(requestSpec.variable(EMAIL, USER_EMAIL)).thenReturn(requestSpec);
        when(requestSpec.retrieve(USER)).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(NaisUserTeamMemberships.class)).thenReturn(naisUserTeamMembershipsMono);
        when(naisUserTeamMembershipsMono.block()).thenReturn(null);

        assertThrows(MissingNaisTeamException.class, () -> naisTeamMembershipService.getAllNaisteamsForEmail(USER_EMAIL));
    }

    @Test
    void getAllNaisteamsForEmail_missingEmailThrowsForbiddenOperationException() {
        assertThrows(ForbiddenOperationException.class, () -> naisTeamMembershipService.getAllNaisteamsForEmail(null));

        verifyNoInteractions(naisGraphqlClient);
    }

    @Test
    void getAllNaisteamsForEmail_blankEmailThrowsForbiddenOperationException() {
        assertThrows(ForbiddenOperationException.class, () -> naisTeamMembershipService.getAllNaisteamsForEmail(" "));

        verifyNoInteractions(naisGraphqlClient);
    }
}
