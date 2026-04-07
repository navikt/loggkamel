package no.nav.sikkerhetstjenesten.loggkamel.rest;

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
@Schema(requiredProperties = {"naisteam","teknologi","dbname, okonomi, personvern, arkiv, fiksa"}, example = """
  {"id": 2, "naisteam": "owning-team", "teknologi": "POSTGRESQL", "dbname": "db-to-backup", "okonomi": true, "arkiv": false, "personvern": false, "fiksa": false, "created": "2026-03-30T10:36:47.331075Z",
      "updated": "2026-03-30T10:36:47.331075Z", "loggingLeseoperasjoner": false, "loggingEndringer": true}""")
public class BackupTaskDTO {
    Long id;
    
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

    Instant created;

    Instant updated;

    Boolean loggingLeseoperasjoner;

    Boolean loggingEndringer;
}
