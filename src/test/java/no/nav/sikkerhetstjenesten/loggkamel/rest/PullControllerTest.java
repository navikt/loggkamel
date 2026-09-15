package no.nav.sikkerhetstjenesten.loggkamel.rest;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import no.nav.sikkerhetstjenesten.loggkamel.service.DB2PacketService;
import no.nav.sikkerhetstjenesten.loggkamel.service.PullRerunService;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.time.LocalDate;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.rest.PullController.SIKKERHETSTJENESTEN_NAISTEAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullControllerTest {

    private static final String EMAIL = "user@nav.no";
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
    NaisService naisService;

    @Mock
    OAuth2AuthenticatedPrincipal principal;

    @Mock
    PullRerunRequiredDTO rerun;

    @Mock
    AuditloggTaskDTO auditloggTask;

    @InjectMocks
    PullController controller;

    private void memberOfSikkerhetstjenesten() {
        when(principal.getAttribute(NaisService.EMAIL_CLAIM)).thenReturn(EMAIL);
        when(naisService.getAllNaisteamsForEmail(EMAIL)).thenReturn(List.of(SIKKERHETSTJENESTEN_NAISTEAM));
    }

    private void notMemberOfSikkerhetstjenesten() {
        when(principal.getAttribute(NaisService.EMAIL_CLAIM)).thenReturn(EMAIL);
        when(naisService.getAllNaisteamsForEmail(EMAIL)).thenReturn(List.of("team-a"));
    }

    @Test
    void getUnresolvedReruns_returnsRerunsWhenCallerIsInSikkerhetstjenesten() {
        memberOfSikkerhetstjenesten();
        when(pullRerunService.findAllUnresolvedReruns()).thenReturn(List.of(rerun));

        List<PullRerunRequiredDTO> result = controller.getUnresolvedReruns(principal);

        assertEquals(List.of(rerun), result);
    }

    @Test
    void getUnresolvedReruns_throwsForbiddenWhenCallerIsNotInSikkerhetstjenesten() {
        notMemberOfSikkerhetstjenesten();

        assertThrows(ForbiddenOperationException.class, () -> controller.getUnresolvedReruns(principal));

        verifyNoInteractions(pullRerunService);
    }

    @Test
    void resolveFailedPull_marksTheEntryResolvedWithoutRerunningThePull() {
        memberOfSikkerhetstjenesten();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenReturn(rerun);

        controller.resolveFailedPull(RERUN_ID, principal);

        verify(pullRerunService).markRerunResolved(RERUN_ID);
        verifyNoInteractions(db2PacketService);
    }

    @Test
    void resolveFailedPull_propagatesEntryNotFoundException() {
        memberOfSikkerhetstjenesten();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenThrow(new PullRerunEntryNotFoundException("not found"));

        assertThrows(PullRerunEntryNotFoundException.class, () -> controller.resolveFailedPull(RERUN_ID, principal));

        verify(pullRerunService, never()).markRerunResolved(any());
    }

    @Test
    void resolveFailedPull_throwsForbiddenWhenCallerIsNotInSikkerhetstjenesten() {
        notMemberOfSikkerhetstjenesten();

        assertThrows(ForbiddenOperationException.class, () -> controller.resolveFailedPull(RERUN_ID, principal));

        verifyNoInteractions(pullRerunService);
    }

    @Test
    void rerunFailedPull_reRunsThePullAndMarksTheEntryResolvedOnSuccess() {
        memberOfSikkerhetstjenesten();
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
        memberOfSikkerhetstjenesten();
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
        memberOfSikkerhetstjenesten();
        when(pullRerunService.findUnresolvedRerunById(RERUN_ID)).thenReturn(rerun);
        when(rerun.getTeknologi()).thenReturn(TeknologiEnum.ORACLE);

        assertThrows(UnsupportedPullTeknologiException.class, () -> controller.rerunFailedPull(RERUN_ID, principal));

        verifyNoInteractions(db2PacketService);
        verify(pullRerunService, never()).markRerunResolved(any());
    }

    @Test
    void rerunFailedPull_throwsForbiddenWhenCallerIsNotInSikkerhetstjenesten() {
        notMemberOfSikkerhetstjenesten();

        assertThrows(ForbiddenOperationException.class, () -> controller.rerunFailedPull(RERUN_ID, principal));

        verifyNoInteractions(pullRerunService);
        verifyNoInteractions(db2PacketService);
    }
}
