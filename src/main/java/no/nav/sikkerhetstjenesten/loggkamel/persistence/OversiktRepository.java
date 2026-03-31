package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OversiktRepository extends JpaRepository<BackupTaskEntity, Long> {
    BackupTaskEntity findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);

    List<BackupTaskEntity> findAllByNaisteam(String naisteam);
}