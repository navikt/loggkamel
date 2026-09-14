package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.sikkerhetstjenesten.loggkamel.persistence.database.PullRerunJPAAdapter.MAX_FAILURE_REASON_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRerunJPAAdapterTest {

    private static final String DBNAME = "dbName";
    private static final TeknologiEnum TEKNOLOGI = TeknologiEnum.DB2;
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 12);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 13);
    private static final String FAILURE_REASON = "java.lang.RuntimeException: proxy unavailable";

    @Mock
    PullRerunRequiredRepository pullRerunRequiredRepository;

    @Mock
    OversiktRepository oversiktRepository;

    @Spy
    PullRerunRequiredMapper mapper = new PullRerunRequiredMapperImpl();

    @InjectMocks
    PullRerunJPAAdapter adapter;

    @Test
    void registerFailedPull_createsNewUnresolvedEntryWhenNoneExists() {
        AuditloggTaskEntity task = auditloggTaskEntity();
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(task);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(task, START_DATE, END_DATE)).thenReturn(Optional.empty());

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, FAILURE_REASON);

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());

        PullRerunRequiredEntity saved = captor.getValue();
        assertEquals(task, saved.getAuditloggTask());
        assertEquals(START_DATE, saved.getPullStartDate());
        assertEquals(END_DATE, saved.getPullEndDate());
        assertEquals(FAILURE_REASON, saved.getFailureReason());
        assertFalse(saved.getResolved());
    }

    @Test
    void registerFailedPull_updatesExistingUnresolvedEntryInsteadOfDuplicating() {
        AuditloggTaskEntity task = auditloggTaskEntity();
        PullRerunRequiredEntity existing = PullRerunRequiredEntity.builder()
                .id(5L)
                .auditloggTask(task)
                .pullStartDate(START_DATE)
                .pullEndDate(END_DATE)
                .failureReason("an older failure")
                .resolved(false)
                .build();

        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(task);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(task, START_DATE, END_DATE)).thenReturn(Optional.of(existing));

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, FAILURE_REASON);

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());

        PullRerunRequiredEntity saved = captor.getValue();
        assertEquals(5L, saved.getId());
        assertEquals(FAILURE_REASON, saved.getFailureReason());
    }

    @Test
    void registerFailedPull_truncatesOverlongFailureReason() {
        AuditloggTaskEntity task = auditloggTaskEntity();
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(task);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(task, START_DATE, END_DATE)).thenReturn(Optional.empty());

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, "x".repeat(MAX_FAILURE_REASON_LENGTH + 100));

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());

        assertEquals(MAX_FAILURE_REASON_LENGTH, captor.getValue().getFailureReason().length());
    }

    @Test
    void registerFailedPull_acceptsNullFailureReason() {
        AuditloggTaskEntity task = auditloggTaskEntity();
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(task);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(task, START_DATE, END_DATE)).thenReturn(Optional.empty());

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, null);

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());

        assertNull(captor.getValue().getFailureReason());
    }

    @Test
    void registerFailedPull_throwsWhenNoArkivTaskExists() {
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, FAILURE_REASON));

        verify(pullRerunRequiredRepository, never()).save(any());
    }

    @Test
    void findAllUnresolvedReruns_mapsTaskIdentityOntoDTO() {
        PullRerunRequiredEntity entity = PullRerunRequiredEntity.builder()
                .id(7L)
                .auditloggTask(auditloggTaskEntity())
                .pullStartDate(START_DATE)
                .pullEndDate(END_DATE)
                .failureReason(FAILURE_REASON)
                .resolved(false)
                .build();
        when(pullRerunRequiredRepository.findAllUnresolved()).thenReturn(List.of(entity));

        List<PullRerunRequiredDTO> unresolved = adapter.findAllUnresolvedReruns();

        assertEquals(1, unresolved.size());
        PullRerunRequiredDTO dto = unresolved.getFirst();
        assertEquals(7L, dto.getId());
        assertEquals(DBNAME, dto.getDbname());
        assertEquals(TEKNOLOGI, dto.getTeknologi());
        assertEquals(START_DATE, dto.getPullStartDate());
        assertEquals(END_DATE, dto.getPullEndDate());
        assertFalse(dto.getResolved());
    }

    @Test
    void markRerunResolved_setsResolvedFlag() {
        PullRerunRequiredEntity entity = PullRerunRequiredEntity.builder()
                .id(7L)
                .auditloggTask(auditloggTaskEntity())
                .pullStartDate(START_DATE)
                .pullEndDate(END_DATE)
                .resolved(false)
                .build();
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.of(entity));

        adapter.markRerunResolved(7L);

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());
        assertTrue(captor.getValue().getResolved());
    }

    @Test
    void markRerunResolved_throwsWhenEntryDoesNotExist() {
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> adapter.markRerunResolved(7L));
    }

    private AuditloggTaskEntity auditloggTaskEntity() {
        return AuditloggTaskEntity.builder()
                .id(1L)
                .naisteam("sikkerhetstjenesten")
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .okonomi(true)
                .endringerUtenKrav(false)
                .loggingLeseoperasjoner(false)
                .fiksa(true)
                .funnetLogger(true)
                .discardLogs(false)
                .build();
    }
}
