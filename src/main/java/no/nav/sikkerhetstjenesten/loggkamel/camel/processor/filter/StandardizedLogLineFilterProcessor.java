package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogLineOperationTypes;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.PLACE_IN_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.MESSAGE_SHOULD_BE_SKIPPED;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class StandardizedLogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(StandardizedLogLineFilterProcessor.class);

    public boolean messageIsMissingImmediateSkipHeader(Exchange exchange) {

        if (exchange.getVariable(MESSAGE_SHOULD_BE_SKIPPED, Boolean.class) == Boolean.TRUE) {
            return false;
        }

        return true;
    }

    public boolean doesLineActionMatchRelevantAuditloggArkiv(Exchange exchange) {
        log.debug("LogLineFilterProcessor called for logfile: {}, line: {}", exchange.getMessage().getHeader(FILE_NAME), exchange.getVariable(PLACE_IN_PACKET));

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
