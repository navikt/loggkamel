package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class LogGroupFilterProcessor {

    @Autowired
    private OversiktService oversiktService;

    private static final Logger log = LoggerFactory.getLogger(LogGroupFilterProcessor.class);

    public boolean isMatchingAuditLoggArkivFound(Exchange exchange) {
        log.info("LogGroupFilterProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME, String.class));

        String filename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (filename == null || !filename.contains(".")) {
            log.warn("Filename header is missing or does not contain expected format: {}", filename);
            return false;
        }

        //database name is the first part of the filename, before the first period
        String dbname = filename.split("\\.")[0];

        TeknologiEnum teknologi = exchange.getProperty(TEKNOLOGI, TeknologiEnum.class);

        AuditLoggArkivResponseDTO auditLoggArkivResponseDTO = oversiktService.getAuditLoggArkivByDbnameAndTeknologi(dbname, teknologi);

        //TODO: also stop processing here if the backup isn't fiksa, or if all of the backup fields are false
        if (auditLoggArkivResponseDTO == null) {
            log.info("No audit logg arkiv found for database {} and teknologi {}, filtering out log line", dbname, teknologi.name());
            return false;
        }

        exchange.setProperty(AUDIT_LOGG_ARKIV, auditLoggArkivResponseDTO);

        return true;
    }
}
