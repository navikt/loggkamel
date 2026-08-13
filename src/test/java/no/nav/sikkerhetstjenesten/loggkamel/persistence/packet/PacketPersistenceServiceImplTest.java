package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacketPersistenceServiceImplTest {

    private static final String PACKET_BUCKET_NAME = "packet bucket name";
    private static final String FILENAME = "I am a name for a file";
    private static final String AUDITLOGG_LINES_AS_JSON = "I am totally a json blob";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(packetPersistenceService, "nativePacketBucketName", PACKET_BUCKET_NAME);
    }

    @Mock
    Bucket bucket;

    @Mock
    List<AuditloggLineMessage> auditloggLineMessages;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    Storage loggkamelProjectStorage;

    @InjectMocks
    PacketPersistenceServiceImpl packetPersistenceService;

    @Test
    void saveAuditloggLineMessagesWithFilename_nullOrEmptyList() {
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, null);
        packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, Collections.emptyList());

        verifyNoInteractions(objectMapper, loggkamelProjectStorage);
    }

    @Test
    void saveAuditloggLineMessagesWithFilename_convertsJsonExceptionToInternal() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(auditloggLineMessages)).thenThrow(new JsonProcessingException("oh noooo"){});

        assertThrows(InvalidLogStreamException.class, () -> packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, auditloggLineMessages));
    }

    @Test
    void saveAuditloggLineMessagesWithFilename_happyPath() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(auditloggLineMessages)).thenReturn(AUDITLOGG_LINES_AS_JSON);
        when(loggkamelProjectStorage.get(anyString())).thenReturn(bucket);

        packetPersistenceService.saveAuditloggLineMessagesWithFilename(FILENAME, auditloggLineMessages);

        verify(bucket).create(FILENAME, AUDITLOGG_LINES_AS_JSON.getBytes(StandardCharsets.UTF_8));
    }

}