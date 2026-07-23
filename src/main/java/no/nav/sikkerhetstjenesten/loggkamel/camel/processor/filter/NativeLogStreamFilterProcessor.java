package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.AUDITLOGG_TASK;

@Service
public class NativeLogStreamFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(NativeLogStreamFilterProcessor.class);

    public boolean doesAuditloggTaskRequireForwardingLogs(Exchange exchange) {
        AuditloggTaskDTO auditloggTaskDTO = exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class);

        if (!auditloggTaskDTO.getFiksa()) {
            throw new InvalidLogStreamException("Logs provided for a database with an auditlogg task that isn't enabled, database: " + auditloggTaskDTO.getDbname() + ". Sending to invalid messages queue");
        }

        if (!auditloggTaskDTO.getLoggingLeseoperasjoner() && !auditloggTaskDTO.getLoggingEndringer()) {
            log.info("Auditlogg task found for database {} and teknologi {}, but logging isn't enabled, filtering out log stream", auditloggTaskDTO.getDbname(), auditloggTaskDTO.getTeknologi().name());
            return false;
        }

        return true;
    }
}
