package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PacketPersistenceServiceImpl implements PacketPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PacketPersistenceServiceImpl.class);

    @Override
    public void saveAuditloggLineMessagesWithFilename(String filename, List<AuditloggLineMessage> auditloggLineMessages) {
        if (auditloggLineMessages == null || auditloggLineMessages.isEmpty()) {
            return;
        }

        //FOR TESTING
        log.info("Here is where I would save the packet with filename {}, contents {}",  filename, auditloggLineMessages);

        //TODO: save as a file in the packet bucket or file location
    }
}
