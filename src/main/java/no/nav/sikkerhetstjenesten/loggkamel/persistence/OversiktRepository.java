package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface OversiktRepository extends JpaRepository<Oversikt, Long> {
    Oversikt findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);
}