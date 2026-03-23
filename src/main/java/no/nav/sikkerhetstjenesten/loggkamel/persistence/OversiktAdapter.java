package no.nav.sikkerhetstjenesten.loggkamel.persistence;

public interface OversiktAdapter {

    OversiktEntity save(OversiktEntity entity);

    OversiktEntity findByDbname(String dbname);
}
