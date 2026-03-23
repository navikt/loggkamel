package no.nav.sikkerhetstjenesten.loggkamel.persistence;

public interface OversiktAdapter {

    OversiktEntity save(OversiktEntity entity);

    OversiktEntity findByDbnameAndTeknologi(String dbname, String teknologi);
}
