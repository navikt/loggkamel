package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.LogLineOperationsEnricher;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.LogLineOperationTypes;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.AUDITLOGG_TASK;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader.PLACE_IN_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.MESSAGE_SHOULD_BE_SKIPPED;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class StandardizedLogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(StandardizedLogLineFilterProcessor.class);

    private final LogLineOperationsEnricher logLineOperationsEnricher;

    @Autowired
    public StandardizedLogLineFilterProcessor(LogLineOperationsEnricher logLineOperationsEnricher) {
        this.logLineOperationsEnricher = logLineOperationsEnricher;
    }

    public boolean messageIsMissingImmediateSkipHeader(Exchange exchange) {
        if (exchange.getVariable(MESSAGE_SHOULD_BE_SKIPPED, Boolean.class) == Boolean.TRUE) {
            return false;
        }

        return true;
    }

    public boolean doesLineActionMatchRelevantAuditloggTask(Exchange exchange) {
        log.debug("LogLineFilterProcessor called for logfile: {}, line: {}", exchange.getMessage().getHeader(FILE_NAME), exchange.getVariable(PLACE_IN_PACKET));

        AuditloggTaskDTO auditloggTaskDTO = exchange.getVariable(AUDITLOGG_TASK, AuditloggTaskDTO.class);

        EnrichedAuditlogg enrichedAuditlogg = exchange.getMessage().getBody(EnrichedAuditlogg.class);
        LogLineOperationTypes routingAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(enrichedAuditlogg.getPgAuditClass());

        if (auditloggTaskDTO.getLoggingLeseoperasjoner() && routingAttributes.isRead()) {
            return true;
        }

        if (auditloggTaskDTO.getLoggingEndringer() && routingAttributes.isModification()) {
            return true;
        }

        return false;
    }
}
