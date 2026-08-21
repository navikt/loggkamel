package no.nav.sikkerhetstjenesten.loggkamel.client.dto;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class DB2AuditloggLineDTO {

    @NonNull
    LocalDateTime metricsTimestamp;

    @NonNull
    String databaseName;

    @NonNull
    String tableName;

    @NonNull
    String authId;

    @NonNull
    String sqlQuery;
}
