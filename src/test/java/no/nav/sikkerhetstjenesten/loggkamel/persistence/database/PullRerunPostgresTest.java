package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PullRerunPostgresTest extends PostgresRepositoryTestBase {

    private static final LocalDate START = LocalDate.of(2026, 1, 10);
    private static final LocalDate END = LocalDate.of(2026, 1, 11);

    @Autowired
    OversiktRepository oversiktRepository;

    @Autowired
    PullRerunRequiredRepository rerunRepository;

    @Autowired
    PullRerunJPAAdapter adapter;

    private AuditloggTaskEntity task;

    @BeforeEach
    void createTask() {
        task = oversiktRepository.saveAndFlush(task("db-a", TeknologiEnum.DB2).build());
    }

    @Test
    void registeringFailedPullStoresUnresolvedRerunWithTaskDetails() {
        adapter.registerFailedPull("db-a", TeknologiEnum.DB2, START, END, "proxy unavailable");

        List<PullRerunRequiredDTO> reruns = adapter.findAllUnresolvedReruns();

        assertThat(reruns).hasSize(1);
        PullRerunRequiredDTO rerun = reruns.getFirst();
        assertThat(rerun.getDbname()).isEqualTo("db-a");
        assertThat(rerun.getTeknologi()).isEqualTo(TeknologiEnum.DB2);
        assertThat(rerun.getPullStartDate()).isEqualTo(START);
        assertThat(rerun.getPullEndDate()).isEqualTo(END);
        assertThat(rerun.getFailureReason()).isEqualTo("proxy unavailable");
        assertThat(rerun.getResolved()).isFalse();
    }

    @Test
    void registeringSameRangeAgainUpdatesExistingRowInsteadOfAddingOne() {
        adapter.registerFailedPull("db-a", TeknologiEnum.DB2, START, END, "first failure");
        adapter.registerFailedPull("db-a", TeknologiEnum.DB2, START, END, "second failure");

        List<PullRerunRequiredDTO> reruns = adapter.findAllUnresolvedReruns();

        assertThat(reruns).hasSize(1);
        assertThat(reruns.getFirst().getFailureReason()).isEqualTo("second failure");
    }

    @Test
    void failureReasonIsTruncatedToColumnLength() {
        adapter.registerFailedPull("db-a", TeknologiEnum.DB2, START, END, "x".repeat(2500));

        assertThat(adapter.findAllUnresolvedReruns().getFirst().getFailureReason())
                .hasSize(PullRerunJPAAdapter.MAX_FAILURE_REASON_LENGTH);
    }

    @Test
    void registeringForUnknownTaskFailsWithoutWritingAnything() {
        assertThatThrownBy(() -> adapter.registerFailedPull("missing", TeknologiEnum.DB2, START, END, "reason"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(adapter.findAllUnresolvedReruns()).isEmpty();
    }

    @Test
    void resolvedRerunsAreExcludedAndSameRangeCanBeRegisteredAgain() {
        adapter.registerFailedPull("db-a", TeknologiEnum.DB2, START, END, "first");
        Long firstId = adapter.findAllUnresolvedReruns().getFirst().getId();

        adapter.markRerunResolved(firstId);
        assertThat(adapter.findAllUnresolvedReruns()).isEmpty();

        adapter.registerFailedPull("db-a", TeknologiEnum.DB2, START, END, "second");

        assertThat(adapter.findAllUnresolvedReruns()).hasSize(1);
        assertThat(adapter.findAllUnresolvedReruns().getFirst().getId()).isNotEqualTo(firstId);
    }

    @Test
    void databaseRejectsSecondUnresolvedRowForSameTaskAndRange() {
        rerunRepository.saveAndFlush(rerun(false));

        assertThatThrownBy(() -> rerunRepository.saveAndFlush(rerun(false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseAllowsResolvedDuplicatesOfSameRange() {
        rerunRepository.saveAndFlush(rerun(true));
        rerunRepository.saveAndFlush(rerun(true));

        assertThat(rerunRepository.count()).isEqualTo(2);
    }

    @Test
    void findUnresolvedForTaskAndRangeMatchesExactRangeOnly() {
        rerunRepository.saveAndFlush(rerun(false));

        assertThat(rerunRepository.findUnresolvedForTaskAndRange(task, START, END)).isPresent();
        assertThat(rerunRepository.findUnresolvedForTaskAndRange(task, START, END.plusDays(1))).isEmpty();
    }

    @Test
    void taskWithRerunsCannotBeDeleted() {
        rerunRepository.saveAndFlush(rerun(false));

        assertThatThrownBy(() -> oversiktRepository.deleteById(task.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PullRerunRequiredEntity rerun(boolean resolved) {
        return PullRerunRequiredEntity.builder()
                .auditloggTask(task)
                .pullStartDate(START)
                .pullEndDate(END)
                .resolved(resolved)
                .build();
    }
}
