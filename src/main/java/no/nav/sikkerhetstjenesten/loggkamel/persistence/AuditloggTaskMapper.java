package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class AuditloggTaskMapper {

    @Mapping(target = "loggingEndringer", source = ".", qualifiedByName = "loggingEndringer")
    public abstract AuditloggTaskDTO auditloggTaskEntityToDTO(AuditloggTaskEntity entity);

    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "funnetLogger", constant = "false")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fiksa", source = "teknologi", qualifiedByName = "setFiksaByTeknologi")
    public abstract AuditloggTaskEntity auditloggTaskRequestDTOToEntity(AuditloggTaskRequestDTO dto);

    //TODO: Move the logic mapping database flags to reads or modifications into its own class, logic doesn't belong in the mapper
    @Named("loggingEndringer")
    public boolean loggingEndringer(AuditloggTaskEntity entity) {
        return entity.getOkonomi() || entity.getArkivlov();
    }

    @Named("setFiksaByTeknologi")
    public boolean setFiksaByTeknologi(TeknologiEnum teknologiEnum) {
        if (teknologiEnum == TeknologiEnum.POSTGRESQL || teknologiEnum == TeknologiEnum.DB2) {
            return true;
        }

        return false;
    }

}
