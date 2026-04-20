package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogLineOperationTypes;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogLineFilterProcessor.class);

    public boolean doesLineActionMatchRelevantAuditloggArkiv(Exchange exchange) {
        log.info("LogLineFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME));

        AuditloggArkivResponseDTO auditloggArkivResponseDTO = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class);
        LogLineOperationTypes routingAttributes = exchange.getVariable(LogLineOperationTypes.LOG_LINE_OPERATION_TYPES, LogLineOperationTypes.class);

        if (auditloggArkivResponseDTO.getLoggingLeseoperasjoner() && routingAttributes.isRead()) {
            return true;
        }

        if (auditloggArkivResponseDTO.getLoggingEndringer() && routingAttributes.isModification()) {
            return true;
        }

        return false;
    }
}
