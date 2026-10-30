package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackfillStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonMapper jackson3Mapper = JsonMapper.builder().build();

    @ParameterizedTest
    @CsvSource({
            "BACKFILL_REQUESTED, backfill-requested",
            "BACKFILL_COMPLETED, backfill-completed",
            "COLD_STORAGE_REQUESTED, cold-storage-requested",
            "COLD_STORAGE_COMPLETED, cold-storage-completed"
    })
    void serializesAndDeserializesAllowedValues(BackfillStatus status, String value) throws Exception {
        assertEquals("\"" + value + "\"", objectMapper.writeValueAsString(status));
        assertEquals(status, objectMapper.readValue("\"" + value + "\"", BackfillStatus.class));
    }

    @Test
    void acceptsAllowedValueInRequestDto() {
        String json = """
                {"naisteam": "t", "teknologi": "POSTGRESQL", "dbname": "db", "okonomi": true, "arkivforskrift": true,
                 "endringerUtenKrav": false, "loggingLeseoperasjoner": false, "backfill": "cold-storage-requested"}
                """;
        assertEquals(BackfillStatus.COLD_STORAGE_REQUESTED,
                jackson3Mapper.readValue(json, AuditloggTaskRequestDTO.class).getBackfill());
    }
}
