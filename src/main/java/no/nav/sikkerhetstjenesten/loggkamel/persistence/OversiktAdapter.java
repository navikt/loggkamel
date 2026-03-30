package no.nav.sikkerhetstjenesten.loggkamel.persistence;

public interface OversiktAdapter {

    BackupTask save(BackupTask entity);

    BackupTask findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi);
}
