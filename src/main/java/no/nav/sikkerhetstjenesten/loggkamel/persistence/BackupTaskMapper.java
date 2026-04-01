package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.controller.BackupTaskDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class BackupTaskMapper {

    @Mapping(target = "loggingLeseoperasjoner", source = ".", qualifiedByName = "loggingLeseoperasjoner")
    @Mapping(target = "loggingEndringer", source = ".", qualifiedByName = "loggingEndringer")
    public abstract BackupTaskDTO backupTaskEntityToDTO(BackupTaskEntity entity);

    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    public abstract BackupTaskEntity backupTaskDTOToEntity(BackupTaskDTO dto);

    //TODO: Move the logic mapping database flags to reads or modifications into its own class, logic doesn't belong in the mapper
    @Named("loggingLeseoperasjoner")
    public boolean loggingLeseoperasjoner(BackupTaskEntity entity) {
        return entity.getArkiv();
    }

    @Named("loggingEndringer")
    public boolean loggingEndringer(BackupTaskEntity entity) {
        return entity.getOkonomi() || entity.getPersonvern();
    }

}
