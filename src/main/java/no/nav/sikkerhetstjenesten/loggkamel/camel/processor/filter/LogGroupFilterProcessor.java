package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.filter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DatabaseDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.AUDIT_LOGG_ARKIV;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
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

        AuditLoggArkivResponseDTO auditLoggArkivResponseDTO;
        try {
            auditLoggArkivResponseDTO = oversiktService.getAuditLoggArkivByDbnameAndTeknologi(dbname, teknologi);
        } catch (RuntimeException e) {
            log.warn("Error while fetching audit logg arkiv for database {} and teknologi {}. Error message: {}", dbname, teknologi.name(), e.getMessage());
            throw new DatabaseDependencyException("Error while fetching audit logg arkiv for database " + dbname + " and teknologi " + teknologi.name(), e);
        }

        if (auditLoggArkivResponseDTO == null) {
            log.info("No audit logg arkiv found for database {} and teknologi {}, filtering out log line", dbname, teknologi.name());
            return false;
        }

        if (!auditLoggArkivResponseDTO.getFiksa() || (!auditLoggArkivResponseDTO.getLoggingLeseoperasjoner() && !auditLoggArkivResponseDTO.getLoggingEndringer())) {
            log.info("Audit logg arkiv found for database {} and teknologi {}, but arkiv configuration isn't complete or logging isn't enabled, filtering out log line", dbname, teknologi.name());
            return false;
        }

        exchange.setProperty(AUDIT_LOGG_ARKIV, auditLoggArkivResponseDTO);

        return true;
    }
}
