package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OversiktJPAAdapter {

    private final OversiktRepository repository;
    private final AuditLoggArkivMapper mapper;

    @Autowired
    public OversiktJPAAdapter(OversiktRepository repository, AuditLoggArkivMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public AuditLoggArkivDTO createAuditLoggArkiv(AuditLoggArkivDTO dto) {
        AuditLoggArkivEntity savedTask = saveAuditLoggArkivEntity(mapper.auditLoggArkivDTOToEntity(dto));
        return mapper.auditLoggArkivEntityToDTO(savedTask);
    }

    private AuditLoggArkivEntity saveAuditLoggArkivEntity(AuditLoggArkivEntity toSave) {
        try {
            return repository.save(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenOperationException("Task with dbname " + toSave.getDbname() + " and teknologi " + toSave.getTeknologi() + " already exists");
        }
    }

    public AuditLoggArkivDTO updateAuditLoggArkiv(AuditLoggArkivDTO dto) {
        if (dto.getId() == null || dto.getId() == 0) {
            throw new ForbiddenOperationException("Id must be provided when updating a task");
        }

        repository.findById(dto.getId()).orElseThrow(() -> new ForbiddenOperationException("Task with id " + dto.getId() + " does not exist"));

        AuditLoggArkivEntity savedTask = saveAuditLoggArkivEntity(mapper.auditLoggArkivDTOToEntity(dto));
        return mapper.auditLoggArkivEntityToDTO(savedTask);
    }

    public AuditLoggArkivDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return mapper.auditLoggArkivEntityToDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public List<AuditLoggArkivDTO> getAllTasksByNaisteam(String naisteam) {
        List<AuditLoggArkivEntity> foundEntities = repository.findAllByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::auditLoggArkivEntityToDTO).toList();
    }

}
