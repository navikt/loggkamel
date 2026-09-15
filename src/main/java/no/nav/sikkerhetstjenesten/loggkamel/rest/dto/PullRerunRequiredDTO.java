package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;

import java.time.Instant;
import java.time.LocalDate;

@Value
@Builder
@Jacksonized
@Schema(requiredProperties = {"id", "dbname", "teknologi", "pullStartDate", "pullEndDate", "resolved"}, example = """
        {
            "id": 42,
            "dbname": "not-a-real-db",
            "teknologi": "DB2",
            "pullStartDate": "2026-09-12",
            "pullEndDate": "2026-09-13",
            "failureReason": "no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException: proxy unavailable",
            "resolved": false,
            "created": "2026-09-13T06:00:04.750377Z",
            "updated": "2026-09-13T06:00:04.750377Z"
        }""", description = "En feilet loggpull som må kjøres på nytt manuelt")
public class PullRerunRequiredDTO {

    @NonNull
    Long id;

    @NonNull
    String dbname;

    @NonNull
    TeknologiEnum teknologi;

    @NonNull
    LocalDate pullStartDate;

    @NonNull
    LocalDate pullEndDate;

    String failureReason;

    @NonNull
    Boolean resolved;

    Instant created;

    Instant updated;
}
