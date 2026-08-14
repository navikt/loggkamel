package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalPacketPersistenceServiceTest {

    private static final String PACKET_BUCKET_NAME = "src/main/resources/testing";
    private static final String FILENAME = "testfilename.json";
    private static final String AUDITLOGG_LINES_AS_JSON_STRING = "totes a json string";

    @Mock
    AuditloggLineMessage auditloggLineMessage;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    LocalPacketPersistenceService packetPersistenceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(packetPersistenceService, "nativePacketBucketName", PACKET_BUCKET_NAME);
    }

    @Test
    void saveAuditloggLineMessagesWithFilename_emptyArgumentsReturnEarly() {
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, null);
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, Collections.emptyList());

        verifyNoInteractions(objectMapper);
    }

    @Test
    void saveAuditloggLineMessagesWithFilename_exceptionWhenMappingConvertsToInvalidLogStream() throws JsonProcessingException {
        List<AuditloggLineMessage> auditloggLineMessages = List.of(auditloggLineMessage);

        when(objectMapper.writeValueAsString(auditloggLineMessages)).thenThrow(new JsonProcessingException("blerg"){});

        assertThrows(InvalidLogStreamException.class, () -> packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, List.of(auditloggLineMessage)));
    }

    @Test
    void saveAuditloggLineMessagesWithFilename_exceptionWhenSavingFileConvertsToInvalidLogStream() throws JsonProcessingException {
        List<AuditloggLineMessage> auditloggLineMessages = List.of(auditloggLineMessage);

        when(objectMapper.writeValueAsString(auditloggLineMessages)).thenReturn(AUDITLOGG_LINES_AS_JSON_STRING);

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.createDirectories(any(Path.class)))
                    .thenThrow(new IOException("boom"));

            assertThrows(InvalidLogStreamException.class, () ->
                    packetPersistenceService.saveAuditloggLineMessagesWithFilename(
                            FILENAME, auditloggLineMessages));
        }
    }

    @Test
    void saveAuditloggLineMessagesWithFilename_happyPath() throws JsonProcessingException {
        List<AuditloggLineMessage> auditloggLineMessages = List.of(auditloggLineMessage);

        when(objectMapper.writeValueAsString(auditloggLineMessages)).thenReturn(AUDITLOGG_LINES_AS_JSON_STRING);

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, auditloggLineMessages);

            Path packetDirectory = Path.of(PACKET_BUCKET_NAME);
            Path packetFilePath = packetDirectory.resolve(FILENAME);
            files.verify(() -> Files.createDirectories(packetDirectory));
            files.verify(() -> Files.writeString(packetFilePath, AUDITLOGG_LINES_AS_JSON_STRING, UTF_8));
        }
    }

}