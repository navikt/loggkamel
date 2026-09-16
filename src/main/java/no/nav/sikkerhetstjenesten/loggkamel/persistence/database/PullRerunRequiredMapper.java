package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.PullRerunRequiredDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class PullRerunRequiredMapper {

    @Mapping(target = "dbname", source = "auditloggTask.dbname")
    @Mapping(target = "teknologi", source = "auditloggTask.teknologi")
    public abstract PullRerunRequiredDTO pullRerunRequiredEntityToDTO(PullRerunRequiredEntity entity);
}
