package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class AuditLoggArkivMapper {

    @Mapping(target = "loggingLeseoperasjoner", source = ".", qualifiedByName = "loggingLeseoperasjoner")
    @Mapping(target = "loggingEndringer", source = ".", qualifiedByName = "loggingEndringer")
    public abstract AuditLoggArkivDTO auditLoggArkivEntityToDTO(AuditLoggArkivEntity entity);

    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract AuditLoggArkivEntity auditLoggArkivDTOToEntity(AuditLoggArkivDTO dto);

    //TODO: Move the logic mapping database flags to reads or modifications into its own class, logic doesn't belong in the mapper
    @Named("loggingLeseoperasjoner")
    public boolean loggingLeseoperasjoner(AuditLoggArkivEntity entity) {
        return entity.getArkiv();
    }

    @Named("loggingEndringer")
    public boolean loggingEndringer(AuditLoggArkivEntity entity) {
        return entity.getOkonomi() || entity.getPersonvern();
    }

}
