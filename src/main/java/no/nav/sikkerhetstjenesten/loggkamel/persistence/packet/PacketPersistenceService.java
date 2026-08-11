package no.nav.sikkerhetstjenesten.loggkamel.persistence.packet;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;

import java.util.List;

public interface PacketPersistenceService {

    void saveAuditloggLineMessagesWithFilename(String filename, List<AuditloggLineMessage> auditloggLineMessages);
}
