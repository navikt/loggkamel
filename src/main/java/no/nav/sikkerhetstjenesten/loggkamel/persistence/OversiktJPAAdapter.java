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

    public AuditLoggArkivResponseDTO createAuditLoggArkiv(AuditLoggArkivRequestDTO arkivToSave) {
        AuditLoggArkivEntity persistedArkiv = saveAuditLoggArkivEntity(mapper.auditLoggArkivRequestDTOToEntity(arkivToSave));
        return mapper.auditLoggArkivEntityToResponseDTO(persistedArkiv);
    }

    private AuditLoggArkivEntity saveAuditLoggArkivEntity(AuditLoggArkivEntity toSave) {
        try {
            return repository.save(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenOperationException("Task with dbname " + toSave.getDbname() + " and teknologi " + toSave.getTeknologi() + " already exists");
        }
    }

    public AuditLoggArkivResponseDTO updateAuditLoggArkiv(AuditLoggArkivRequestDTO arkivToUpdate) {

        AuditLoggArkivEntity existingArkiv = repository.findByDbnameAndTeknologi(arkivToUpdate.getDbname(), arkivToUpdate.getTeknologi());
        if (existingArkiv == null) {
            throw new ForbiddenOperationException("Task with dbname " + arkivToUpdate.getDbname() + " and teknologi " + arkivToUpdate.getTeknologi() + " does not exist");
        }
        AuditLoggArkivEntity arkivEntityWithUpdatedValues = mapper.auditLoggArkivRequestDTOToEntity(arkivToUpdate);
        arkivEntityWithUpdatedValues.setId(existingArkiv.getId());

        AuditLoggArkivEntity persistedArkiv = saveAuditLoggArkivEntity(arkivEntityWithUpdatedValues);
        return mapper.auditLoggArkivEntityToResponseDTO(persistedArkiv);
    }

    public AuditLoggArkivResponseDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        //TODO: wrap in try/catch, convert exceptions to something uniform for route error handling
        return mapper.auditLoggArkivEntityToResponseDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public List<AuditLoggArkivResponseDTO> getAllTasksByNaisteam(String naisteam) {
        List<AuditLoggArkivEntity> foundEntities = repository.findAllByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::auditLoggArkivEntityToResponseDTO).toList();
    }

}
