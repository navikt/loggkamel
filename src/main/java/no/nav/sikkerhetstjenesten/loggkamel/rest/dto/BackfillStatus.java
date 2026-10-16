package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BackfillStatus {
    BACKFILL_REQUESTED("backfill-requested"),
    BACKFILL_COMPLETED("backfill-completed"),
    COLD_STORAGE_REQUESTED("cold-storage-requested"),
    COLD_STORAGE_COMPLETED("cold-storage-completed");

    private final String value;

    BackfillStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BackfillStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported backfill status: " + value));
    }
}
