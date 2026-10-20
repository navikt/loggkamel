package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.NAISTEAMS_CLAIM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NaisteamControllerTest {

    @Mock
    AuditloggTaskService service;

    @Mock
    OAuth2AuthenticatedPrincipal principal;

    NaisteamController controller;

    @BeforeEach
    void setUp() {
        controller = new NaisteamController(service);
    }

    @Test
    void mineReturnsTeamsFromPrincipal() {
        List<String> teams = List.of("team-a", "team-b");
        when(principal.getAttribute(NAISTEAMS_CLAIM)).thenReturn(teams);

        assertEquals(teams, controller.findNaisteamsForCurrentUser(principal));

        verifyNoInteractions(service);
    }

    @Test
    void mineReturnsEmptyListForMissingTeamClaim() {
        assertEquals(List.of(), controller.findNaisteamsForCurrentUser(principal));
        verifyNoInteractions(service);
    }

    @Test
    void mineReturnsEmptyListForNullPrincipal() {
        assertEquals(List.of(), controller.findNaisteamsForCurrentUser(null));
        verifyNoInteractions(service);
    }
}
