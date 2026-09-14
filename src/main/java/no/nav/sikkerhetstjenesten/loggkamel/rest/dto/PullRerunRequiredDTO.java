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
@Schema(description = "En feilet loggpull som må kjøres på nytt manuelt")
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
