package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BackfillStatus {
    FALSE("false"),
    REQUESTED("requested"),
    FINISHED("finished");

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
