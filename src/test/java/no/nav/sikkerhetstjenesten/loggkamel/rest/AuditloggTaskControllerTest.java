package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.NaisteamDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuditloggTaskControllerTest {

    private static final List<String> TEAMS = List.of("team-a", "team-b");

    @Mock
    AuditloggTaskService service;

    @Mock
    OAuth2AuthenticatedPrincipal principal;

    @Mock
    AuditloggTaskRequestDTO request;

    @Mock
    AuditloggTaskDTO task;

    AuditloggTaskController controller;

    @BeforeEach
    void setUp() {
        controller = securedController(service);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPassesPrincipalTeamsToOwnershipCheck() {
        authenticate(USER_AUTHORITY);
        when(principal.getAttribute(NAISTEAMS_CLAIM)).thenReturn(TEAMS);
        when(service.createAuditloggTask(request, TEAMS)).thenReturn(task);

        assertSame(task, controller.createAuditloggTask(request, principal));
        verify(service).createAuditloggTask(request, TEAMS);
    }

    @Test
    void updatePassesPrincipalTeamsToOwnershipCheck() {
        authenticate(USER_AUTHORITY);
        when(principal.getAttribute(NAISTEAMS_CLAIM)).thenReturn(TEAMS);
        when(service.updateAuditloggTask(request, TEAMS)).thenReturn(task);

        assertSame(task, controller.updateAuditloggTask(request, principal));
        verify(service).updateAuditloggTask(request, TEAMS);
    }

    @Test
    void mineUsesPrincipalTeams() {
        authenticate(USER_AUTHORITY);
        when(principal.getAttribute(NAISTEAMS_CLAIM)).thenReturn(TEAMS);
        List<NaisteamDTO> groupedTasks = List.of(mock(NaisteamDTO.class));
        when(service.getTasksGroupedByNaisteam(TEAMS)).thenReturn(groupedTasks);

        assertSame(groupedTasks, controller.getAuditloggTasksForCurrentUser(principal));
    }

    @Test
    void missingTeamClaimIsPassedAsEmptyMembership() {
        authenticate(USER_AUTHORITY);

        controller.createAuditloggTask(request, principal);
        controller.updateAuditloggTask(request, principal);
        controller.getAuditloggTasksForCurrentUser(principal);

        verify(service).createAuditloggTask(request, List.of());
        verify(service).updateAuditloggTask(request, List.of());
        verify(service).getTasksGroupedByNaisteam(List.of());
    }

    @Test
    void nullPrincipalIsPassedAsEmptyMembership() {
        authenticate(USER_AUTHORITY);

        controller.createAuditloggTask(request, null);
        controller.updateAuditloggTask(request, null);
        controller.getAuditloggTasksForCurrentUser(null);

        verify(service).createAuditloggTask(request, List.of());
        verify(service).updateAuditloggTask(request, List.of());
        verify(service).getTasksGroupedByNaisteam(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {SERVICE_AUTHORITY, ADMIN_AUTHORITY})
    void callersWithoutUserAuthorityCannotEnterTaskMethods(String authority) {
        authenticate(authority);

        assertThrows(AccessDeniedException.class, () -> controller.createAuditloggTask(request, principal));
        assertThrows(AccessDeniedException.class, () -> controller.updateAuditloggTask(request, principal));
        assertThrows(AccessDeniedException.class, () -> controller.getAuditloggTasksForCurrentUser(principal));

        verifyNoInteractions(service);
    }

    @Test
    void unauthenticatedCallerCannotEnterTaskMethods() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.createAuditloggTask(request, principal));
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.updateAuditloggTask(request, principal));
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.getAuditloggTasksForCurrentUser(principal));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void taskWriteEndpointsRejectConflictingChangeFlags(String httpMethod) throws Exception {
        HttpMethod method = HttpMethod.valueOf(httpMethod);
        var mvc = MockMvcBuilders.standaloneSetup(new AuditloggTaskController(service))
                .setControllerAdvice(new RestExceptionInterceptor())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        String json = """
                {"naisteam":"team-a","teknologi":"POSTGRESQL","dbname":"db",
                 "okonomi":true,"arkivforskrift":false,"endringerUtenKrav":true,
                 "loggingLeseoperasjoner":false,"backfill":"cold-storage-completed"}
                """;
        mvc.perform(request(method, "/api/v1/task").contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400));
        verifyNoInteractions(service);
    }

    private AuditloggTaskController securedController(AuditloggTaskService taskService) {
        ProxyFactory factory = new ProxyFactory(new AuditloggTaskController(taskService));
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.secured());
        return (AuditloggTaskController) factory.getProxy();
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
