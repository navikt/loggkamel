package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
import no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.LogRoutingAttributes;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogLineFilterProcessor.class);

    public boolean doesLineActionMatchRelevantAuditLoggArkiv(Exchange exchange) {
        log.info("LogFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME));

        AuditLoggArkivDTO auditLoggArkivDTO = exchange.getProperty(AUDIT_LOGG_ARKIV, AuditLoggArkivDTO.class);
        LogRoutingAttributes routingAttributes = exchange.getProperty(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES, LogRoutingAttributes.class);

        if (auditLoggArkivDTO.getPersonvern() && routingAttributes.isRead()) {
            return true;
        }

        if ((auditLoggArkivDTO.getArkiv() || auditLoggArkivDTO.getOkonomi()) && routingAttributes.isModification()) {
            return true;
        }

        return false;
    }
}
