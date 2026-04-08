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
@Schema(requiredProperties = {"naisteam","teknologi","dbname, okonomi, personvern, arkiv, fiksa, created, updated, loggingLeseoperasjoner, loggingEndringer"}, example = """
  {"naisteam": "owning-team", "teknologi": "POSTGRESQL", "dbname": "db-to-arkiv", "okonomi": true, "arkiv": false, "personvern": false, "fiksa": false, "created": "2026-03-30T10:36:47.331075Z",
      "updated": "2026-03-30T10:36:47.331075Z", "loggingLeseoperasjoner": false, "loggingEndringer": true}""")
public class AuditLoggArkivResponseDTO {
    @NonNull
    String naisteam;
    
    @NonNull
    TeknologiEnum teknologi;
    
    @NonNull
    String dbname;
    
    @NonNull
    Boolean okonomi;
    
    @NonNull
    Boolean arkiv;
    
    @NonNull
    Boolean personvern;
    
    @NonNull
    Boolean fiksa;

//    @NonNull
    Instant created;

//    @NonNull
    Instant updated;

    @NonNull
    Boolean loggingLeseoperasjoner;

    @NonNull
    Boolean loggingEndringer;
}
