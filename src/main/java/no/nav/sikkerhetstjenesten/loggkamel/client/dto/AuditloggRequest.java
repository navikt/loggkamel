package no.nav.sikkerhetstjenesten.loggkamel.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class AuditloggRequest {
    @NonNull
    String databaseName;

    @NonNull
    LocalDateTime logStartTime;

    @NonNull
    LocalDateTime logEndTime;
}
