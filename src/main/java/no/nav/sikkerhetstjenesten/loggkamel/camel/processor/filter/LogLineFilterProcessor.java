package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogLineRoutingAttributes;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.AUDITLOGG_ARKIV;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogLineFilterProcessor.class);

    public boolean doesLineActionMatchRelevantAuditloggArkiv(Exchange exchange) {
        log.info("LogFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME));

        AuditloggArkivResponseDTO auditloggArkivResponseDTO = exchange.getProperty(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class);
        LogLineRoutingAttributes routingAttributes = exchange.getVariable(LogLineRoutingAttributes.LOG_ROUTING_ATTRIBUTES, LogLineRoutingAttributes.class);

        if (auditloggArkivResponseDTO.getLoggingLeseoperasjoner() && routingAttributes.isRead()) {
            return true;
        }

        if (auditloggArkivResponseDTO.getLoggingEndringer() && routingAttributes.isModification()) {
            return true;
        }

        return false;
    }
}
