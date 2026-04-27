package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OversiktJPAAdapter {

    private final OversiktRepository repository;
    private final AuditloggArkivMapper mapper;

    @Autowired
    public OversiktJPAAdapter(OversiktRepository repository, AuditloggArkivMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public AuditloggArkivResponseDTO createAuditloggArkiv(AuditloggArkivRequestDTO arkivToSave) {
        AuditloggArkivEntity persistedArkiv = saveAuditloggArkivEntity(mapper.auditloggArkivRequestDTOToEntity(arkivToSave));
        return mapper.auditloggArkivEntityToResponseDTO(persistedArkiv);
    }

    private AuditloggArkivEntity saveAuditloggArkivEntity(AuditloggArkivEntity toSave) {
        try {
            return repository.save(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ForbiddenOperationException("Task with dbname " + toSave.getDbname() + " and teknologi " + toSave.getTeknologi() + " already exists");
        }
    }

    public AuditloggArkivResponseDTO updateAuditloggArkiv(AuditloggArkivRequestDTO arkivToUpdate) {

        AuditloggArkivEntity existingArkiv = repository.findByDbnameAndTeknologi(arkivToUpdate.getDbname(), arkivToUpdate.getTeknologi());
        if (existingArkiv == null) {
            throw new ForbiddenOperationException("Task with dbname " + arkivToUpdate.getDbname() + " and teknologi " + arkivToUpdate.getTeknologi() + " does not exist");
        }
        AuditloggArkivEntity arkivEntityWithUpdatedValues = mapper.auditloggArkivRequestDTOToEntity(arkivToUpdate);
        arkivEntityWithUpdatedValues.setId(existingArkiv.getId());

        AuditloggArkivEntity persistedArkiv = saveAuditloggArkivEntity(arkivEntityWithUpdatedValues);
        return mapper.auditloggArkivEntityToResponseDTO(persistedArkiv);
    }

    public AuditloggArkivResponseDTO findByDbnameAndTeknologi(String dbname, TeknologiEnum teknologi) {
        return mapper.auditloggArkivEntityToResponseDTO(repository.findByDbnameAndTeknologi(dbname, teknologi));
    }

    public void registerLogsReceivedForAuditloggArkiv(String dbname, TeknologiEnum teknologi) {
        AuditloggArkivEntity toUpdate = repository.findByDbnameAndTeknologi(dbname, teknologi);
        toUpdate.setFunnetLogger(true);
        saveAuditloggArkivEntity(toUpdate);
    }

    public List<AuditloggArkivResponseDTO> getAllTasksByNaisteam(String naisteam) {
        List<AuditloggArkivEntity> foundEntities = repository.findAllByNaisteam(naisteam);

        return foundEntities.stream().map(mapper::auditloggArkivEntityToResponseDTO).toList();
    }

}
