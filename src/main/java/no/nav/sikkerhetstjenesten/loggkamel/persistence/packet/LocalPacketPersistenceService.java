package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@ConditionalOnLocalOrTest
public class LocalPacketPersistenceService implements PacketPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(LocalPacketPersistenceService.class);

    @Value("${routing.packet.bucket-name:#{''}}")
    private String nativePacketBucketName;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public void saveAuditloggLineMessagesWithFilename(String filename, List<AuditloggLineMessage> auditloggLineMessages) {
        if (auditloggLineMessages == null || auditloggLineMessages.isEmpty()) {
            return;
        }

        log.info("Saving DB2 packet with filename {}",  filename);

        String auditloggLineMessagesAsString;
        try {
            auditloggLineMessagesAsString = objectMapper.writeValueAsString(auditloggLineMessages);
        } catch (JsonProcessingException e) {
            throw new InvalidLogStreamException("Failure when converting DB2 auditloggLineMessages to JSON", e);
        }

        Path packetDirectory = Path.of(nativePacketBucketName);
        Path packetFilePath = packetDirectory.resolve(filename);
        try {
            Files.createDirectories(packetDirectory);
            Files.writeString(packetFilePath, auditloggLineMessagesAsString, UTF_8);
        } catch (IOException e) {
            throw new InvalidLogStreamException("Failure when writing DB2 packet to local filesystem", e);
        }

        log.info("Successfully saved DB2 packet with filename {}", filename);
    }
}
