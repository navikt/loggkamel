package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DatabaseDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.NaisService;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogEnrichmentValues.*;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class PostgresLogGroupEnrichmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PostgresLogGroupEnrichmentProcessor.class);

    private final NaisService naisService;
    private final OversiktService oversiktService;

    @Autowired
    public PostgresLogGroupEnrichmentProcessor(NaisService naisService, OversiktService oversiktService) {
        this.naisService = naisService;
        this.oversiktService = oversiktService;
    }

    public void enrich(Exchange exchange) {
        log.info("PostgresLogGroupEnrichmentProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME, String.class));

        String filename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (filename == null || !filename.contains(".")) {
            log.warn("Filename header is missing or does not contain expected format: {}", filename);
            throw new InvalidPostgresLogGroupException("Filename header is missing or does not contain expected format: " + filename);
        }

        //database name is the first part of the filename, before the first period
        String dbname = filename.split("\\.")[0];

        TeknologiEnum teknologi = exchange.getProperty(TEKNOLOGI, TeknologiEnum.class);

        AuditloggArkivResponseDTO auditloggArkivResponseDTO = getAuditloggArkiv(dbname, teknologi);

        if (auditloggArkivResponseDTO == null) {
            log.info("No audit logg arkiv found for database {} and teknologi {}, filtering out log line", dbname, teknologi.name());
            throw new InvalidPostgresLogGroupException("No audit logg arkiv found for database " + dbname + " and teknologi " + teknologi.name());
        }

        log.debug("Found auditloggArkiv, setting properties for log enrichment: {}", auditloggArkivResponseDTO);
        exchange.setProperty(AUDITLOGG_ARKIV, auditloggArkivResponseDTO);

        String teamGcpProjectId = naisService.getCurrentEnvGCPIDForTeam(auditloggArkivResponseDTO.getNaisteam());
        log.debug("Found GCP project id {} for team {}, setting property for log enrichment", teamGcpProjectId, auditloggArkivResponseDTO.getNaisteam());
        exchange.setProperty(TEAM_GCP_PROJECT_ID, teamGcpProjectId);
    }

    private AuditloggArkivResponseDTO getAuditloggArkiv(String dbname, TeknologiEnum teknologi) {
        try {
            return oversiktService.getAuditloggArkivByDbnameAndTeknologi(dbname, teknologi);
        } catch (RuntimeException e) {
            log.warn("Error while fetching audit logg arkiv for database {} and teknologi {}. Error message: {}", dbname, teknologi.name(), e.getMessage());
            throw new DatabaseDependencyException("Error while fetching audit logg arkiv for database " + dbname + " and teknologi " + teknologi.name(), e);
        }
    }
}
