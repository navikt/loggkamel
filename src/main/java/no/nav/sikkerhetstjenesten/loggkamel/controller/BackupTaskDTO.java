package no.nav.sikkerhetstjenesten.loggkamel.controller;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
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

    private static String sanitizeSqlInput(String input) {
        if (input == null) {
            return null;
        }
        // Escape single quotes by doubling them (SQL standard)
        return input.replace("'", "''");
    }

    //TODO: more in-depth sanitization here? And how to implement?
    /**
     * Creates a new BackupTaskRequest with SQL-sanitized naisteam and dbname fields.
     * Note: This method is typically not needed when deserializing from JSON/REST,
     * as the custom deserializer automatically sanitizes these fields.
     * Use this method when constructing BackupTaskRequest programmatically.
     *
     * @return a new instance with sanitized SQL fields
     */
    public BackupTaskDTO withSanitizedSqlFields() {
        return this.toBuilder()
            .naisteam(sanitizeSqlInput(this.naisteam))
            .dbname(sanitizeSqlInput(this.dbname))
            .build();
    }
}
