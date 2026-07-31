package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;

import java.time.Instant;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
@Schema(requiredProperties = {"naisteam","teknologi","dbname, okonomi, loggingLeseoperasjoner, arkivlov, fiksa, created, updated, loggingEndringer, funnetLogger"}, example = """
        {
             "naisteam": "sikkerhetstjenesten",
             "teknologi": "POSTGRESQL",
             "dbname": "sikkerhets-test",
             "okonomi": true,
             "arkivlov": true,
             "loggingLeseoperasjoner": true,
             "fiksa": true,
             "created": "2026-04-13T14:07:02.863834Z",
             "updated": "2026-04-29T10:30:11.275180Z",
             "loggingEndringer": true,
             "funnetLogger": true
         }""", description = "funnetLogger = om loggkamel har sett logginnslag fra den databasen, fiksa = fått tilgang til å hente loggene til databasen")
public class AuditloggTaskDTO {

    @NonNull
    String naisteam;
    
    @NonNull
    TeknologiEnum teknologi;
    
    @NonNull
    String dbname;
    
    @NonNull
    Boolean okonomi;
    
    @NonNull
    Boolean arkivlov;
    
    @NonNull
    Boolean loggingLeseoperasjoner;
    
    @NonNull
    Boolean fiksa;

    Instant created;

    Instant updated;

    @NonNull
    Boolean loggingEndringer;

    @NonNull
    Boolean funnetLogger;
}
