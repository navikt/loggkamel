package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
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

    public AuditLoggArkivResponseDTO createAuditLoggArkiv(AuditLoggArkivRequestDTO dto) {
        AuditLoggArkivEntity savedTask = saveAuditLoggArkivEntity(mapper.auditLoggArkivRequestDTOToEntity(dto));
        return mapper.auditLoggArkivEntityToResponseDTO(savedTask);
    }

    private AuditLoggArkivEntity saveAuditLoggArkivEntity(AuditLoggArkivEntity toSave) {
        try {
            return repository.save(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenOperationException("Task with dbname " + toSave.getDbname() + " and teknologi " + toSave.getTeknologi() + " already exists");
        }
    }

    public AuditLoggArkivResponseDTO updateAuditLoggArkiv(AuditLoggArkivRequestDTO dto) {

        AuditLoggArkivEntity existingArkiv = repository.findByDbnameAndTeknologi(dto.getDbname(), dto.getTeknologi());
        if (existingArkiv == null) {
            throw new ForbiddenOperationException("Task with dbname " + dto.getDbname() + " and teknologi " + dto.getTeknologi() + " does not exist");
        }
        AuditLoggArkivEntity updatedArkiv = mapper.auditLoggArkivRequestDTOToEntity(dto);
        updatedArkiv.setId(existingArkiv.getId());

        AuditLoggArkivEntity savedTask = saveAuditLoggArkivEntity(updatedArkiv);
        return mapper.auditLoggArkivEntityToResponseDTO(savedTask);
    }

    public AuditLoggArkivResponseDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return mapper.auditLoggArkivEntityToResponseDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public List<AuditLoggArkivResponseDTO> getAllTasksByNaisteam(String naisteam) {
        List<AuditLoggArkivEntity> foundEntities = repository.findAllByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::auditLoggArkivEntityToResponseDTO).toList();
    }

}
