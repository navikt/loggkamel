package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
@Schema(requiredProperties = {"naisteam", "tasksForTeam"}, example = """
        {
            "naisteam": "other-team",
            "tasksForTeam": [
                {
                    "naisteam": "other-team",
                    "teknologi": "POSTGRESQL",
                    "dbname": "not-a-real-db",
                    "okonomi": false,
                    "arkivlov": false,
                    "loggingLeseoperasjoner": false,
                    "fiksa": true,
                    "created": "2026-07-31T08:39:42.750377Z",
                    "updated": "2026-07-31T08:39:42.750377Z",
                    "loggingEndringer": false,
                    "funnetLogger": false
                }
            ]
        }""", description = "en gruppering av AuditloggTaskDTO, gruppert på naisteam")
public class NaisTeamDTO {

    @NonNull
    String naisteam;

    @NonNull
    List<AuditloggTaskDTO> tasksForTeam;
}
