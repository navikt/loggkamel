package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class PacketPersistenceServiceImpl implements PacketPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PacketPersistenceServiceImpl.class);

    @Autowired
    ObjectMapper  objectMapper;

//    @Value()
    private String nativePacketBucketURI = "gs://loggkamel-dev-logglines";

    @Override
    public void saveAuditloggLineMessagesWithFilename(String filename, List<AuditloggLineMessage> auditloggLineMessages) {
        if (auditloggLineMessages == null || auditloggLineMessages.isEmpty()) {
            return;
        }

        //FOR TESTING
        log.info("Attempting to save the packet with filename {}, contents {}",  filename, auditloggLineMessages);

        Storage loggkamelProjectStorage = StorageOptions.getDefaultInstance().getService();
        Bucket bucketForNativePackets = loggkamelProjectStorage.get(nativePacketBucketURI);

        //DEBUG
        log.info("Successfully connected to project storage and logglines bucket");

        String auditloggLineMessagesAsString = null;
        try {
            auditloggLineMessagesAsString = objectMapper.writeValueAsString(auditloggLineMessages);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        byte[] auditloggLineMessagesAsBytes = auditloggLineMessagesAsString.getBytes(UTF_8);

        //DEBUG
        log.info("Successfully converted the List<AuditloggLineMessage> to JSON string");

        bucketForNativePackets.create(filename, auditloggLineMessagesAsBytes);

        //DEBUG
        log.info("Successfully uploaded the file");
    }
}
