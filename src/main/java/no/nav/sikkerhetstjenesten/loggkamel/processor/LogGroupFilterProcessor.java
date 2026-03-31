package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.controller.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.BACKUP_TASK;
import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogGroupFilterProcessor {

    @Autowired
    private OversiktService oversiktService;

    private static final Logger log = LoggerFactory.getLogger(LogGroupFilterProcessor.class);

    public boolean isMatchingBackupTaskFound(Exchange exchange) {
        log.info("LogGroupFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME, String.class));

        String filename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (filename == null || !filename.contains(".")) {
            log.warn("Filename header is missing or does not contain expected format: {}", filename);
            return false;
        }

        //database name is the first part of the filename, before the first period
        String dbname = filename.split("\\.")[0];

        TeknologiEnum teknologi = exchange.getProperty(TEKNOLOGI, TeknologiEnum.class);

        BackupTaskDTO backupTaskDTO = oversiktService.getOversiktByDbnameAndTeknologi(dbname, teknologi);

        if (backupTaskDTO == null) {
            log.info("No backup task found for database {} and teknologi {}, filtering out log line", dbname, teknologi.name());
            return false;
        }

        exchange.setProperty(BACKUP_TASK, backupTaskDTO);

        return true;
    }
}
