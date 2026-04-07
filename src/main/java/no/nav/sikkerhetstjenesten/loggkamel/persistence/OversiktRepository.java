package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OversiktRepository extends JpaRepository<AuditLoggArkivEntity, Long> {
    AuditLoggArkivEntity findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);

    List<AuditLoggArkivEntity> findAllByNaisteam(String naisteam);
}