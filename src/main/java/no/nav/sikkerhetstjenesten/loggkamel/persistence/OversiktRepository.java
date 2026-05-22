package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface OversiktRepository extends JpaRepository<AuditloggArkivEntity, Long> {
    AuditloggArkivEntity findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);

    List<AuditloggArkivEntity> findAllArkivByNaisteam(String naisteam);

    @Query("SELECT DISTINCT e.naisteam FROM AuditloggArkivEntity e WHERE e.naisteam IS NOT NULL")
    List<String> findAllDistinctNaisteam();
}