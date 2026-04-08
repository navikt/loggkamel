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

        AuditLoggArkivEntity existingArkiv = repository.findByDbnameAndTeknologi(dto.getDbname(), dto.getTeknologi());
        if (existingArkiv == null) {
            throw new ForbiddenOperationException("Task with dbname " + dto.getDbname() + " and teknologi " + dto.getTeknologi() + " does not exist");
        }
        AuditLoggArkivEntity updatedArkiv = mapper.auditLoggArkivDTOToEntity(dto);
        updatedArkiv.setId(existingArkiv.getId());

        AuditLoggArkivEntity savedTask = saveAuditLoggArkivEntity(updatedArkiv);
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
