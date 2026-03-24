package no.nav.sikkerhetstjenesten.loggkamel.persistence;

public interface OversiktAdapter {

    Oversikt save(Oversikt entity);

    Oversikt findByDbnameAndTeknologi(String dbname, String teknologi);
}
