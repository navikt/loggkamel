package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface OversiktRepository extends JpaRepository<AuditloggTaskEntity, Long> {
    AuditloggTaskEntity findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);

    List<AuditloggTaskEntity> findAllTasksByNaisteam(String naisteam);

    @Query("SELECT DISTINCT e.naisteam FROM AuditloggTaskEntity e WHERE e.naisteam IS NOT NULL")
    List<String> findAllDistinctNaisteam();
}