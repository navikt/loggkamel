package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.controller.BackupTaskDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OversiktJPAAdapter {

    private final OversiktRepository repository;
    private final BackupTaskMapper mapper;

    @Autowired
    public OversiktJPAAdapter(OversiktRepository repository, BackupTaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public BackupTaskDTO createBackupTask(BackupTaskDTO dto) {
        BackupTaskEntity toSave = mapper.backupTaskDTOToEntity(dto);

        //TODO: catch DataIntegrityViolationException for attempt to duplicate dbname and teknologi, throw exception that is converted by interceptor into meaningful response
        toSave = repository.save(toSave);

        return mapper.backupTaskEntityToDTO(toSave);
    }

    public BackupTaskDTO updateBackupTask(BackupTaskDTO dto) {
        BackupTaskEntity toUpdate = mapper.backupTaskDTOToEntity(dto);

        // TODO: check that task with given id exists, if not throw exception that is converted by interceptor into meaningful response
        //TODO: catch DataIntegrityViolationException for attempt to duplicate dbname and teknologi, throw exception that is converted by interceptor into meaningful response
        toUpdate = repository.save(toUpdate);

        return mapper.backupTaskEntityToDTO(toUpdate);
    }

    public BackupTaskDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return mapper.backupTaskEntityToDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public List<BackupTaskDTO> getAllTasksByNaisteam(String naisteam) {
        List<BackupTaskEntity> foundEntities = repository.findAllByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::backupTaskEntityToDTO).toList();
    }

}
