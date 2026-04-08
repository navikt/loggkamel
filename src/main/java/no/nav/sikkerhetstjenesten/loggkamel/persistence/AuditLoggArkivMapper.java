package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class AuditLoggArkivMapper {

    @Mapping(target = "loggingEndringer", source = ".", qualifiedByName = "loggingEndringer")
    public abstract AuditLoggArkivResponseDTO auditLoggArkivEntityToResponseDTO(AuditLoggArkivEntity entity);

    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "id", ignore = true)
    public abstract AuditLoggArkivEntity auditLoggArkivRequestDTOToEntity(AuditLoggArkivRequestDTO dto);

    //TODO: Move the logic mapping database flags to reads or modifications into its own class, logic doesn't belong in the mapper
    @Named("loggingEndringer")
    public boolean loggingEndringer(AuditLoggArkivEntity entity) {
        return entity.getOkonomi() || entity.getLoggingLeseoperasjoner();
    }

}
