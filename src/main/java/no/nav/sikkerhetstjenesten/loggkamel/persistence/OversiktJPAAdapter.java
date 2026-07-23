package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OversiktJPAAdapter {

    private final OversiktRepository repository;
    private final AuditloggTaskMapper mapper;

    @Autowired
    public OversiktJPAAdapter(OversiktRepository repository, AuditloggTaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public AuditloggTaskDTO createAuditloggTask(AuditloggTaskRequestDTO taskToSave) {
        AuditloggTaskEntity persistedTask = saveAuditloggTaskEntity(mapper.auditloggTaskRequestDTOToEntity(taskToSave));
        return mapper.auditloggTaskEntityToDTO(persistedTask);
    }

    private AuditloggTaskEntity saveAuditloggTaskEntity(AuditloggTaskEntity toSave) {
        try {
            return repository.save(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenOperationException("Task with dbname " + toSave.getDbname() + " and teknologi " + toSave.getTeknologi() + " already exists");
        }
    }

    public AuditloggTaskDTO updateAuditloggTask(AuditloggTaskRequestDTO taskToUpdate) {

        AuditloggTaskEntity existingTask = repository.findByDbnameAndTeknologi(taskToUpdate.getDbname(), taskToUpdate.getTeknologi());
        if (existingTask == null) {
            throw new ForbiddenOperationException("Task with dbname " + taskToUpdate.getDbname() + " and teknologi " + taskToUpdate.getTeknologi() + " does not exist");
        }
        AuditloggTaskEntity taskEntityWithUpdatedValues = mapper.auditloggTaskRequestDTOToEntity(taskToUpdate);
        taskEntityWithUpdatedValues.setId(existingTask.getId());

        AuditloggTaskEntity persistedTask = saveAuditloggTaskEntity(taskEntityWithUpdatedValues);
        return mapper.auditloggTaskEntityToDTO(persistedTask);
    }

    public AuditloggTaskDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return mapper.auditloggTaskEntityToDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public void registerLogsReceivedForAuditloggTask(String dbname, TeknologiEnum teknologi) {
        AuditloggTaskEntity toUpdate = repository.findByDbnameAndTeknologi(dbname, teknologi);
        toUpdate.setFunnetLogger(true);
        saveAuditloggTaskEntity(toUpdate);
    }

    public List<AuditloggTaskDTO> getAllTasksByNaisteam(String naisteam) {
        List<AuditloggTaskEntity> foundEntities = repository.findAllTasksByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::auditloggTaskEntityToDTO).toList();
    }

    public List<String> findAllDistinctNaisteam() {
        return repository.findAllDistinctNaisteam();
    }

}
