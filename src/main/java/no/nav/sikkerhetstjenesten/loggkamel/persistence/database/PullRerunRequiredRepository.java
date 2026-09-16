package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface PullRerunRequiredRepository extends JpaRepository<PullRerunRequiredEntity, Long> {

    @Query("""
            SELECT e FROM PullRerunRequiredEntity e
            WHERE e.auditloggTask = :auditloggTask
              AND e.pullStartDate = :pullStartDate
              AND e.pullEndDate = :pullEndDate
              AND e.resolved = false
            """)
    Optional<PullRerunRequiredEntity> findUnresolvedForTaskAndRange(AuditloggTaskEntity auditloggTask, LocalDate pullStartDate, LocalDate pullEndDate);

    @Query("SELECT e FROM PullRerunRequiredEntity e WHERE e.resolved = false ORDER BY e.created ASC")
    List<PullRerunRequiredEntity> findAllUnresolved();
}
