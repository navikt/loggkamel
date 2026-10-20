package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2PacketService;
import no.nav.sikkerhetstjenesten.loggkamel.service.PullRerunService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.time.LocalDate;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.ADMIN_AUTHORITY;
import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.USER_AUTHORITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRerunControllerTest {

    private static final Long RERUN_ID = 7L;
    private static final String DBNAME = "dbName";
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 12);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 13);

    @Mock
    PullRerunService pullRerunService;

    @Mock
    AuditloggTaskService auditloggTaskService;

    @Mock
    DB2PacketService db2PacketService;

    @Mock
    OAuth2AuthenticatedPrincipal principal;

    @Mock
    PullRerunRequiredDTO rerun;

    @Mock
    AuditloggTaskDTO auditloggTask;

    PullRerunController controller;

    @BeforeEach
    void setUp() {
        ProxyFactory factory = new ProxyFactory(new PullRerunController(pullRerunService, auditloggTaskService, db2PacketService));
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.secured());
        controller = (PullRerunController) factory.getProxy();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void memberOfAdminTeam() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority(USER_AUTHORITY), new SimpleGrantedAuthority(ADMIN_AUTHORITY))));
    }

    private void notMemberOfAdminTeam() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority(USER_AUTHORITY))));
    }

    @Test
    void getUnresolvedReruns_returnsRerunsWhenCallerIsInAdminTeam() {
        memberOfAdminTeam();
        when(pullRerunService.findAllUnresolvedReruns()).thenReturn(List.of(rerun));

        List<PullRerunRequiredDTO> result = controller.getUnresolvedReruns(principal);

        assertEquals(List.of(rerun), result);
    }

    @Test
    void getUnresolvedReruns_throwsForbiddenWhenCallerIsNotInAdminTeam() {
        notMemberOfAdminTeam();

        assertThrows(AccessDeniedException.class, () -> controller.getUnresolvedReruns(principal));

        verifyNoInteractions(pullRerunService);
    }

    @Test
    void resolveFailedPull_marksTheEntryResolvedWithoutRerunningThePull() {
        memberOfAdminTeam();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenReturn(rerun);

        controller.resolveFailedPull(RERUN_ID, principal);

        verify(pullRerunService).markRerunResolved(RERUN_ID);
        verifyNoInteractions(db2PacketService);
    }

    @Test
    void resolveFailedPull_propagatesEntryNotFoundException() {
        memberOfAdminTeam();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenThrow(new PullRerunEntryNotFoundException("not found"));

        assertThrows(PullRerunEntryNotFoundException.class, () -> controller.resolveFailedPull(RERUN_ID, principal));

        verify(pullRerunService, never()).markRerunResolved(any());
    }

    @Test
    void resolveFailedPull_throwsForbiddenWhenCallerIsNotInAdminTeam() {
        notMemberOfAdminTeam();

        assertThrows(AccessDeniedException.class, () -> controller.resolveFailedPull(RERUN_ID, principal));

        verifyNoInteractions(pullRerunService);
    }

    @Test
    void rerunFailedPull_reRunsThePullAndMarksTheEntryResolvedOnSuccess() {
        memberOfAdminTeam();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenReturn(rerun);
        when(rerun.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(rerun.getDbname()).thenReturn(DBNAME);
        when(rerun.getPullStartDate()).thenReturn(START_DATE);
        when(rerun.getPullEndDate()).thenReturn(END_DATE);
        when(auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggTask);

        controller.rerunFailedPull(RERUN_ID, principal);

        verify(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(auditloggTask, START_DATE, END_DATE);
        verify(pullRerunService).markRerunResolved(RERUN_ID);
        verify(pullRerunService, never()).registerFailedPull(any(), any(), any(), any(), any());
    }

    @Test
    void rerunFailedPull_registersTheFailureAgainAndRethrowsWhenTheRerunAlsoFails() {
        memberOfAdminTeam();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenReturn(rerun);
        when(rerun.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(rerun.getDbname()).thenReturn(DBNAME);
        when(rerun.getPullStartDate()).thenReturn(START_DATE);
        when(rerun.getPullEndDate()).thenReturn(END_DATE);
        when(auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TeknologiEnum.DB2)).thenReturn(auditloggTask);

        RuntimeException pullFailure = new RuntimeException("proxy unavailable");
        doThrow(pullFailure).when(db2PacketService).fetchLogsWithinDateRangeAndPersistAsPackets(auditloggTask, START_DATE, END_DATE);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> controller.rerunFailedPull(RERUN_ID, principal));

        assertEquals(pullFailure, thrown);
        verify(pullRerunService).registerFailedPull(DBNAME, TeknologiEnum.DB2, START_DATE, END_DATE, pullFailure);
        verify(pullRerunService, never()).markRerunResolved(any());
    }

    @Test
    void rerunFailedPull_rejectsUnsupportedTeknologiWithoutAttemptingAPull() {
        memberOfAdminTeam();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenReturn(rerun);
        when(rerun.getTeknologi()).thenReturn(TeknologiEnum.ORACLE);

        assertThrows(UnsupportedPullTeknologiException.class, () -> controller.rerunFailedPull(RERUN_ID, principal));

        verifyNoInteractions(db2PacketService);
        verify(pullRerunService, never()).markRerunResolved(any());
    }

    @Test
    void rerunFailedPull_throwsForbiddenWhenCallerIsNotInAdminTeam() {
        notMemberOfAdminTeam();

        assertThrows(AccessDeniedException.class, () -> controller.rerunFailedPull(RERUN_ID, principal));

        verifyNoInteractions(pullRerunService);
        verifyNoInteractions(db2PacketService);
    }

    @Test
    void unauthenticatedCallerCannotEnterAnyAdminMethod() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.getUnresolvedReruns(principal));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.resolveFailedPull(RERUN_ID, principal));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.rerunFailedPull(RERUN_ID, principal));

        verifyNoInteractions(pullRerunService, auditloggTaskService, db2PacketService);
    }
}
