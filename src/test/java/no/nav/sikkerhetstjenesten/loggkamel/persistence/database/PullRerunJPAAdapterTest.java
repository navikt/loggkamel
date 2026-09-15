package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.PullRerunAlreadyResolvedException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.PullRerunEntryNotFoundException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.sikkerhetstjenesten.loggkamel.persistence.database.PullRerunJPAAdapter.MAX_FAILURE_REASON_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    PullRerunRequiredMapper mapper;

    @Mock
    AuditloggTaskEntity auditloggTaskEntity;

    @Mock
    PullRerunRequiredEntity pullRerunRequiredEntity;

    @Mock
    PullRerunRequiredDTO pullRerunRequiredDTO;

    @InjectMocks
    PullRerunJPAAdapter adapter;

    @Test
    void registerFailedPull_createsNewUnresolvedEntryWhenNoneExists() {
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggTaskEntity);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(auditloggTaskEntity, START_DATE, END_DATE)).thenReturn(Optional.empty());

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, FAILURE_REASON);

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());

        PullRerunRequiredEntity saved = captor.getValue();
        assertEquals(auditloggTaskEntity, saved.getAuditloggTask());
        assertEquals(START_DATE, saved.getPullStartDate());
        assertEquals(END_DATE, saved.getPullEndDate());
        assertEquals(FAILURE_REASON, saved.getFailureReason());
        assertFalse(saved.getResolved());
    }

    @Test
    void registerFailedPull_updatesExistingUnresolvedEntryInsteadOfDuplicating() {
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggTaskEntity);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(auditloggTaskEntity, START_DATE, END_DATE))
                .thenReturn(Optional.of(pullRerunRequiredEntity));

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, FAILURE_REASON);

        verify(pullRerunRequiredEntity).setFailureReason(FAILURE_REASON);
        verify(pullRerunRequiredRepository).save(pullRerunRequiredEntity);
    }

    @Test
    void registerFailedPull_truncatesOverlongFailureReason() {
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggTaskEntity);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(auditloggTaskEntity, START_DATE, END_DATE)).thenReturn(Optional.empty());

        adapter.registerFailedPull(DBNAME, TEKNOLOGI, START_DATE, END_DATE, "x".repeat(MAX_FAILURE_REASON_LENGTH + 100));

        ArgumentCaptor<PullRerunRequiredEntity> captor = ArgumentCaptor.forClass(PullRerunRequiredEntity.class);
        verify(pullRerunRequiredRepository).save(captor.capture());

        assertEquals(MAX_FAILURE_REASON_LENGTH, captor.getValue().getFailureReason().length());
    }

    @Test
    void registerFailedPull_acceptsNullFailureReason() {
        when(oversiktRepository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(auditloggTaskEntity);
        when(pullRerunRequiredRepository.findUnresolvedForTaskAndRange(auditloggTaskEntity, START_DATE, END_DATE)).thenReturn(Optional.empty());

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
        when(pullRerunRequiredRepository.findAllUnresolved()).thenReturn(List.of(pullRerunRequiredEntity));
        when(mapper.pullRerunRequiredEntityToDTO(pullRerunRequiredEntity)).thenReturn(pullRerunRequiredDTO);

        List<PullRerunRequiredDTO> unresolved = adapter.findAllUnresolvedReruns();

        assertEquals(List.of(pullRerunRequiredDTO), unresolved);
        verify(mapper).pullRerunRequiredEntityToDTO(pullRerunRequiredEntity);
    }

    @Test
    void markRerunResolved_setsResolvedFlag() {
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.of(pullRerunRequiredEntity));

        adapter.markRerunResolved(7L);

        verify(pullRerunRequiredEntity).setResolved(true);
        verify(pullRerunRequiredRepository).save(pullRerunRequiredEntity);
    }

    @Test
    void markRerunResolved_throwsWhenEntryDoesNotExist() {
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> adapter.markRerunResolved(7L));
    }

    @Test
    void findUnresolvedRerunById_returnsMappedDTOWhenUnresolved() {
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.of(pullRerunRequiredEntity));
        when(pullRerunRequiredEntity.getResolved()).thenReturn(false);
        when(mapper.pullRerunRequiredEntityToDTO(pullRerunRequiredEntity)).thenReturn(pullRerunRequiredDTO);

        PullRerunRequiredDTO result = adapter.findUnresolvedRerunById(7L);

        assertEquals(pullRerunRequiredDTO, result);
    }

    @Test
    void findUnresolvedRerunById_throwsWhenEntryDoesNotExist() {
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(PullRerunEntryNotFoundException.class, () -> adapter.findUnresolvedRerunById(7L));
    }

    @Test
    void findUnresolvedRerunById_throwsWhenEntryAlreadyResolved() {
        when(pullRerunRequiredRepository.findById(7L)).thenReturn(Optional.of(pullRerunRequiredEntity));
        when(pullRerunRequiredEntity.getResolved()).thenReturn(true);

        assertThrows(PullRerunAlreadyResolvedException.class, () -> adapter.findUnresolvedRerunById(7L));
    }
}
