package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
@Schema(
        requiredProperties = {
                "naisteam",
                "teknologi",
                "dbname",
                "okonomi",
                "arkivforskrift",
                "loggingLeseoperasjoner",
                "endringerUtenKrav",
                "backfill"
        },
        example = """
                {
                    "naisteam": "owning-team",
                    "teknologi": "POSTGRESQL",
                    "dbname": "db-to-arkiv",
                    "okonomi": true,
                    "arkivforskrift": false,
                    "endringerUtenKrav": false,
                    "loggingLeseoperasjoner": false,
                    "backfill": "cold-storage-completed"
                }
                """
)
public class AuditloggTaskRequestDTO {
    @NonNull
    String naisteam;

    @NonNull
    TeknologiEnum teknologi;

    @NonNull
    String dbname;

    @NonNull
    Boolean okonomi;

    @NonNull
    Boolean arkivforskrift;

    @NonNull
    @Schema(description = "Can only be true when okonomi and arkivforskrift are false")
    Boolean endringerUtenKrav;

    @NonNull
    Boolean loggingLeseoperasjoner;

    @NonNull
    BackfillStatus backfill;

    @AssertTrue(message = "endringerUtenKrav can only be true when okonomi and arkivforskrift are false")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isChangeLoggingConfigurationValid() {
        return !endringerUtenKrav || (!okonomi && !arkivforskrift);
    }
}
