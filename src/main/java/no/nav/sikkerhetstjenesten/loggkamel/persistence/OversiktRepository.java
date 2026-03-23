package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface OversiktRepository extends JpaRepository<OversiktEntity, Long> {
    OversiktEntity findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);
}