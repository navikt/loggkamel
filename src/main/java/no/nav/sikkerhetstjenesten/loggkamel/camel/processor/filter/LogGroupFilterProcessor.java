package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_ARKIV;

@Service
public class LogGroupFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogGroupFilterProcessor.class);

    public boolean isMatchingAuditloggArkivFound(Exchange exchange) {
        AuditloggArkivResponseDTO auditloggArkivResponseDTO = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class);

        if (!auditloggArkivResponseDTO.getFiksa() || (!auditloggArkivResponseDTO.getLoggingLeseoperasjoner() && !auditloggArkivResponseDTO.getLoggingEndringer())) {
            log.info("Audit logg arkiv found for database {} and teknologi {}, but arkiv configuration isn't complete or logging isn't enabled, filtering out log line", auditloggArkivResponseDTO.getDbname(), auditloggArkivResponseDTO.getTeknologi().name());
            return false;
        }

        return true;
    }
}
