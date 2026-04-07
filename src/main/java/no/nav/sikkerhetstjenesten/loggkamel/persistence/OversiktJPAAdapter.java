package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
        BackupTaskEntity savedTask = saveBackupTaskEntity(mapper.backupTaskDTOToEntity(dto));
        return mapper.backupTaskEntityToDTO(savedTask);
    }

    private BackupTaskEntity saveBackupTaskEntity(BackupTaskEntity toSave) {
        try {
            return repository.save(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenOperationException("Task with dbname " + toSave.getDbname() + " and teknologi " + toSave.getTeknologi() + " already exists");
        }
    }

    public BackupTaskDTO updateBackupTask(BackupTaskDTO dto) {
        if (dto.getId() == null || dto.getId() == 0) {
            throw new ForbiddenOperationException("Id must be provided when updating a task");
        }

        repository.findById(dto.getId()).orElseThrow(() -> new ForbiddenOperationException("Task with id " + dto.getId() + " does not exist"));

        BackupTaskEntity savedTask = saveBackupTaskEntity(mapper.backupTaskDTOToEntity(dto));
        return mapper.backupTaskEntityToDTO(savedTask);
    }

    public BackupTaskDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return mapper.backupTaskEntityToDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public List<BackupTaskDTO> getAllTasksByNaisteam(String naisteam) {
        List<BackupTaskEntity> foundEntities = repository.findAllByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::backupTaskEntityToDTO).toList();
    }

}
