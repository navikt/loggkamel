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

import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.EMAIL;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.Naisteam;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.NaisteamConnection;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.NaisteamNode;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.NaisUserTeamMemberships;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.TEAM_MEMBERSHIPS_FOR_USER_QUERY;
import static no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisteamMembershipServiceImpl.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaisteamMembershipServiceImplTest {

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
    NaisteamMembershipServiceImpl naisTeamMembershipService;

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
                new NaisteamConnection(List.of(new NaisteamNode(new Naisteam(NAIS_TEAM))))
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
    void requireEmailOnlyRejectsNull() {
        NaisteamMembershipService.requireEmail("");
        NaisteamMembershipService.requireEmail(" ");
        NaisteamMembershipService.requireEmail(USER_EMAIL);
    }
}
