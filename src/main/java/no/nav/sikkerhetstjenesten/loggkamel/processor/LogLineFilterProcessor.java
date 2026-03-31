package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.controller.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.LogRoutingAttributes;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.BACKUP_TASK;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogLineFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogLineFilterProcessor.class);

    public boolean doesLineActionMatchConfiguredBackupTask(Exchange exchange) {
        log.info("LogFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME));

        BackupTaskDTO backupTaskDTO = exchange.getProperty(BACKUP_TASK, BackupTaskDTO.class);
        LogRoutingAttributes routingAttributes = exchange.getProperty(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES, LogRoutingAttributes.class);

        if (backupTaskDTO.getPersonvern() && routingAttributes.isRead()) {
            return true;
        }

        if ((backupTaskDTO.getArkiv() || backupTaskDTO.getOkonomi()) && routingAttributes.isModification()) {
            return true;
        }

        return false;
    }
}
