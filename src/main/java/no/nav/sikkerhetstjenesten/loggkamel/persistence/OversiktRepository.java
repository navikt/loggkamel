package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OversiktRepository extends JpaRepository<AuditloggArkivEntity, Long> {
    AuditloggArkivEntity findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);

    List<AuditloggArkivEntity> findAllByNaisteam(String naisteam);
}