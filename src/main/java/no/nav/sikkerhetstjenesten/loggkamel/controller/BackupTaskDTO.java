package no.nav.sikkerhetstjenesten.loggkamel.controller;

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
  {
              "arkiv": false,
              "dbname": "db-to-backup",
              "fiksa": true,
              "naisteam": "team-that-owns-db-logs",
              "okonomi": false,
              "personvern": false,
              "teknologi": "POSTGRESQL"
          }
""")
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
