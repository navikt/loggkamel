package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DB2DTOMapper {

    private final ObjectMapper objectMapper;

    @Autowired
    public DB2DTOMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AuditloggLineMessage> convertDB2DTOsToAuditloggLineMessages(List<DB2AuditloggLineDTO> db2AuditloggLineDTOs, AuditloggTaskDTO auditloggTaskDTO, String gcpId) {
        List<AuditloggLineMessage> packetAsAuditloggLineMessages =  new ArrayList<>();
        int i = 1;
        for (DB2AuditloggLineDTO db2LogLine : db2AuditloggLineDTOs) {
            AuditloggLineMessage auditloggLineMessage = null;
            try {
                auditloggLineMessage = AuditloggLineMessage.builder()
                        .body(objectMapper.writeValueAsString(db2LogLine))
                        .header(AuditloggLineMessageHeader.builder()
                                .auditloggTaskDTO(auditloggTaskDTO)
                                .teamGcpProjectId(gcpId)
                                .teknologi(TeknologiEnum.DB2)
                                .placeInPacket(i)
                                .build())
                        .build();
            } catch (JsonProcessingException e) {
                throw new InvalidLogLineException("Failed to convert db2LogLine to JSON String", e);
            }

            packetAsAuditloggLineMessages.add(auditloggLineMessage);

            i++;
        }

        return packetAsAuditloggLineMessages;
    }
}
