package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface OversiktRepository extends JpaRepository<BackupTask, Long> {
    BackupTask findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);
}