package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Component
@ConditionalOnGCP
public class GCPPacketPersistenceService implements PacketPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GCPPacketPersistenceService.class);

    @Value("${routing.packet.bucket-name:#{''}}")
    private String nativePacketBucketName;

    private final ObjectMapper  objectMapper;
    private final Storage loggkamelProjectStorage;

    @Autowired
    public GCPPacketPersistenceService(ObjectMapper objectMapper, @Qualifier("serviceLocalGcpStorage") Storage storage) {
        this.objectMapper = objectMapper;
        this.loggkamelProjectStorage = storage;
    }

    @Override
    public void saveAuditloggLineMessagesWithFilename(String filename, List<AuditloggLineMessage> auditloggLineMessages) {
        if (auditloggLineMessages == null || auditloggLineMessages.isEmpty()) {
            return;
        }

        log.info("Saving DB2 packet with filename {}",  filename);

        String auditloggLineMessagesAsString = null;
        try {
            auditloggLineMessagesAsString = objectMapper.writeValueAsString(auditloggLineMessages);
        } catch (JsonProcessingException e) {
            throw new InvalidLogStreamException("Failure when converting DB2 auditloggLineMessages to JSON", e);
        }
        byte[] auditloggLineMessagesAsBytes = auditloggLineMessagesAsString.getBytes(UTF_8);

        BlobInfo blobInfo = BlobInfo.newBuilder(nativePacketBucketName, filename)
                .setContentType(APPLICATION_JSON_VALUE)
                .build();
        loggkamelProjectStorage.create(blobInfo, auditloggLineMessagesAsBytes);

        log.info("Successfully uploaded DB2 packet with filename {}",  filename);
    }
}
