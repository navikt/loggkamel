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
@Schema(requiredProperties = {"naisteam","teknologi","dbname, okonomi, loggingLeseoperasjoner, arkiv, fiksa, created, updated, loggingEndringer"}, example = """
  {"naisteam": "owning-team", "teknologi": "POSTGRESQL", "dbname": "db-to-arkiv", "okonomi": true, "arkiv": false, "loggingLeseoperasjoner": false, "fiksa": false, "created": "2026-03-30T10:36:47.331075Z",
      "updated": "2026-03-30T10:36:47.331075Z", "loggingEndringer": true}""")
public class AuditloggArkivResponseDTO {

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
}
