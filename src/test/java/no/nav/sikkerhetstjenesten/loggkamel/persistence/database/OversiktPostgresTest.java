package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OversiktPostgresTest extends PostgresRepositoryTestBase {

    @Autowired
    OversiktRepository repository;

    @Autowired
    OversiktJPAAdapter adapter;

    @Test
    void findsTaskByDbnameAndTeknologiOnly() {
        repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).build());
        repository.saveAndFlush(task("db-a", TeknologiEnum.DB2).build());
        repository.saveAndFlush(task("db-b", TeknologiEnum.POSTGRESQL).build());

        AuditloggTaskEntity found = repository.findByDbnameAndTeknologi("db-a", TeknologiEnum.DB2);

        assertThat(found.getDbname()).isEqualTo("db-a");
        assertThat(found.getTeknologi()).isEqualTo(TeknologiEnum.DB2);
        assertThat(repository.findByDbnameAndTeknologi("db-a", TeknologiEnum.ORACLE)).isNull();
    }

    @Test
    void sameDbnameWithDifferentTeknologiIsAllowedButExactDuplicateIsRejected() {
        repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).build());
        repository.saveAndFlush(task("db-a", TeknologiEnum.ORACLE).build());

        assertThatThrownBy(() -> repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void adapterCreatesTaskAndTranslatesDuplicateToForbiddenOperation() {
        AuditloggTaskRequestDTO request = AuditloggTaskRequestDTO.builder()
                .naisteam("testteam")
                .teknologi(TeknologiEnum.POSTGRESQL)
                .dbname("db-a")
                .okonomi(true)
                .endringerUtenKrav(false)
                .loggingLeseoperasjoner(false)
                .backfill(BackfillStatus.COLD_STORAGE_COMPLETED)
                .build();

        AuditloggTaskDTO created = adapter.createAuditloggTask(request);

        assertThat(created.getDbname()).isEqualTo("db-a");
        assertThat(created.getTeknologi()).isEqualTo(TeknologiEnum.POSTGRESQL);
        assertThatThrownBy(() -> adapter.createAuditloggTask(request)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void findsTasksByNaisteamAndDistinctNaisteamsExcludingNull() {
        repository.saveAndFlush(task("db-1", TeknologiEnum.POSTGRESQL).naisteam("team-a").build());
        repository.saveAndFlush(task("db-2", TeknologiEnum.POSTGRESQL).naisteam("team-a").build());
        repository.saveAndFlush(task("db-3", TeknologiEnum.POSTGRESQL).naisteam("team-b").build());
        repository.saveAndFlush(task("db-4", TeknologiEnum.POSTGRESQL).naisteam(null).discardLogs(true).build());

        assertThat(repository.findAllTasksByNaisteam("team-a")).extracting(AuditloggTaskEntity::getDbname)
                .containsExactlyInAnyOrder("db-1", "db-2");
        assertThat(repository.findAllDistinctNaisteam()).containsExactlyInAnyOrder("team-a", "team-b");
    }

    @Test
    void configuredTasksAreFilteredOnTeknologiFiksaAndBackfill() {
        repository.saveAndFlush(task("match", TeknologiEnum.DB2).backfill("backfill-requested").build());
        repository.saveAndFlush(task("not-fixed", TeknologiEnum.DB2).fiksa(false).backfill("backfill-requested").build());
        repository.saveAndFlush(task("other-tech", TeknologiEnum.POSTGRESQL).backfill("backfill-requested").build());
        repository.saveAndFlush(task("other-status", TeknologiEnum.DB2).backfill("cold-storage-completed").build());

        assertThat(repository.findAllByTeknologiAndFiksaTrue(TeknologiEnum.DB2)).extracting(AuditloggTaskEntity::getDbname)
                .containsExactlyInAnyOrder("match", "other-status");

        List<AuditloggTaskDTO> configured =
                adapter.findConfiguredTasksByTeknologiAndBackfill(TeknologiEnum.DB2, BackfillStatus.BACKFILL_REQUESTED);
        assertThat(configured).extracting(AuditloggTaskDTO::getDbname).containsExactly("match");
    }

    @Test
    void adapterUpdatesBackfillStatusAndFunnetLogger() {
        repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).build());

        adapter.setBackfillStatus("db-a", TeknologiEnum.POSTGRESQL, BackfillStatus.COLD_STORAGE_REQUESTED);
        adapter.registerLogsReceivedForAuditloggTask("db-a", TeknologiEnum.POSTGRESQL);

        AuditloggTaskEntity reloaded = repository.findByDbnameAndTeknologi("db-a", TeknologiEnum.POSTGRESQL);
        assertThat(reloaded.getBackfill()).isEqualTo("cold-storage-requested");
        assertThat(reloaded.getFunnetLogger()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(BackfillStatus.class)
    void databaseAcceptsEveryBackfillStatusTheApplicationDefines(BackfillStatus status) {
        repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).backfill(status.getValue()).build());

        assertThat(repository.findByDbnameAndTeknologi("db-a", TeknologiEnum.POSTGRESQL).getBackfill())
                .isEqualTo(status.getValue());
    }

    @Test
    void databaseRejectsUnknownBackfillValue() {
        assertThatThrownBy(() -> repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).backfill("nonsense").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void naisteamIsRequiredUnlessLogsAreDiscarded() {
        assertThatThrownBy(() -> repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).naisteam(null).build()))
                .isInstanceOf(DataIntegrityViolationException.class);

        repository.saveAndFlush(task("db-b", TeknologiEnum.POSTGRESQL).naisteam(null).discardLogs(true).build());

        assertThat(repository.findByDbnameAndTeknologi("db-b", TeknologiEnum.POSTGRESQL).getNaisteam()).isNull();
    }

    @Test
    void discardedTasksCannotHaveForwardingTypes() {
        assertThatThrownBy(() -> repository.saveAndFlush(
                task("db-a", TeknologiEnum.POSTGRESQL).naisteam(null).discardLogs(true).okonomi(true).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatedTimestampTriggerAdvancesOnUpdateAndCreatedStaysFixed() {
        AuditloggTaskEntity saved = repository.saveAndFlush(task("db-a", TeknologiEnum.POSTGRESQL).build());
        Instant createdBefore = timestamp("created", saved.getId());
        Instant updatedBefore = timestamp("updated", saved.getId());

        saved.setNaisteam("another-team");
        repository.saveAndFlush(saved);

        assertThat(timestamp("created", saved.getId())).isEqualTo(createdBefore);
        assertThat(timestamp("updated", saved.getId())).isAfter(updatedBefore);
    }

    private Instant timestamp(String column, Long id) {
        return jdbc.queryForObject("SELECT " + column + " FROM oversikt WHERE id = ?", java.sql.Timestamp.class, id).toInstant();
    }
}
