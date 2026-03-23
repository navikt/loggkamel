package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktEntity;
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

    public boolean filterIfDatabaseNotConfiguredForBackup(Exchange exchange) {
        log.info("LogGroupFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME));

        String filename = exchange.getIn().getHeader(FILE_NAME, String.class);

        if (filename == null || !filename.contains(".")) {
            log.warn("Filename header is missing or does not contain expected format: {}", filename);
            return false;
        }

        //database name is the first part of the filename, before the first period
        String dbname = filename.split("\\.")[0];

        String teknologi = exchange.getIn().getHeader(TEKNOLOGI, String.class);

        //TODO: test checking for missing db, make sure it's null here and not an exception
        OversiktEntity backupTask = oversiktService.getOversiktByDbnameAndTeknologi(dbname, teknologi);

        if (backupTask == null) {
            log.info("No backup task found for database {} and teknologi {}, filtering out log line", dbname, teknologi);
            return false;
        }

        exchange.getIn().setHeader(BACKUP_TASK, backupTask);

        return true;
    }
}
