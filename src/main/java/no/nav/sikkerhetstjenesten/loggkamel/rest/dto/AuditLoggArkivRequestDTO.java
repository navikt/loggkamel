package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
@Schema(requiredProperties = {"naisteam","teknologi","dbname, okonomi, loggingLeseoperasjoner, arkiv, fiksa"}, example = """
  {"naisteam": "owning-team", "teknologi": "POSTGRESQL", "dbname": "db-to-arkiv", "okonomi": true, "arkiv": false, "loggingLeseoperasjoner": false, "fiksa": false
  }""")
public class AuditLoggArkivRequestDTO {
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
    Boolean loggingLeseoperasjoner;

    @NonNull
    Boolean fiksa;
}
