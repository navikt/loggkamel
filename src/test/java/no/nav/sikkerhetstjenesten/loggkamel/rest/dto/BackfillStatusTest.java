package no.nav.sikkerhetstjenesten.loggkamel.rest.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackfillStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAsLowerCaseValue() throws Exception {
        assertEquals("\"requested\"", objectMapper.writeValueAsString(BackfillStatus.REQUESTED));
    }

    @Test
    void deserializesAllowedValue() throws Exception {
        assertEquals(BackfillStatus.FINISHED, objectMapper.readValue("\"finished\"", BackfillStatus.class));
    }

    @Test
    void rejectsUnsupportedValue() {
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue("\"invalid_value\"", BackfillStatus.class));
    }
}
