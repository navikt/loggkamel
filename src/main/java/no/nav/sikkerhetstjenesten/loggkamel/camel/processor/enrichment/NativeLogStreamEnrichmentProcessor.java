package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DatabaseDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.NaisService;
import no.nav.sikkerhetstjenesten.loggkamel.service.OversiktService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class NativeLogStreamEnrichmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(NativeLogStreamEnrichmentProcessor.class);

    private final NaisService naisService;
    private final OversiktService oversiktService;

    @Autowired
    public NativeLogStreamEnrichmentProcessor(NaisService naisService, OversiktService oversiktService) {
        this.naisService = naisService;
        this.oversiktService = oversiktService;
    }

    public void enrich(Exchange exchange) {
        log.debug("NativeLogStreamEnrichmentProcessor called for log: {}", exchange.getMessage().getHeader(FILE_NAME, String.class));

        String filename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (filename == null || !filename.contains(".")) {
            log.warn("Filename header is missing or does not contain expected format: {}", filename);
            throw new InvalidLogStreamException("Filename header is missing or does not contain expected format: " + filename);
        }

        //database name is the first part of the filename, before the first period
        String dbname = filename.split("\\.")[0];

        TeknologiEnum teknologi = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class);

        AuditloggTaskDTO auditloggTaskDTO = getAuditloggTask(dbname, teknologi);

        if (auditloggTaskDTO == null) {
            log.info("No auditlogg task found for database {} and teknologi {}, sending to backout queue", dbname, teknologi.name());
            throw new InvalidLogStreamException("No auditlogg task found for database " + dbname + " and teknologi " + teknologi.name());
        }

        log.debug("Found auditloggTask, setting properties for log enrichment: {}", auditloggTaskDTO);
        registerLogsReceivedForAuditloggTask(dbname, teknologi);

        String teamGcpProjectId = naisService.getCurrentEnvGCPIDForTeam(auditloggTaskDTO.getNaisteam());
        if (teamGcpProjectId == null || teamGcpProjectId.isEmpty()) {
            log.info("Could not find GCP project id for naisteam {}, sending log message to backout queue", auditloggTaskDTO.getNaisteam());
            throw new InvalidLogStreamException("Could not find GCP project id for naisteam " + auditloggTaskDTO.getNaisteam());
        }
        log.debug("Found GCP project id {} for team {}, setting property for log enrichment", teamGcpProjectId, auditloggTaskDTO.getNaisteam());

        exchange.setVariable(AUDITLOGG_TASK, auditloggTaskDTO);
        exchange.setVariable(TEAM_GCP_PROJECT_ID, teamGcpProjectId);
    }

    private AuditloggTaskDTO getAuditloggTask(String dbname, TeknologiEnum teknologi) {
        try {
            return oversiktService.getAuditloggTaskByDbnameAndTeknologi(dbname, teknologi);
        } catch (RuntimeException e) {
            log.warn("Error while fetching auditlogg task for database {} and teknologi {}. Error message: {}", dbname, teknologi.name(), e.getMessage());
            throw new DatabaseDependencyException("Error while fetching auditlogg task for database " + dbname + " and teknologi " + teknologi.name(), e);
        }
    }

    private void registerLogsReceivedForAuditloggTask(String dbname, TeknologiEnum teknologi) {
        try {
            oversiktService.registerLogsReceivedForAuditloggTask(dbname, teknologi);
        } catch (RuntimeException e) {
            log.warn("Error while registering logs received for database {} and teknologi {}. Error message: {}", dbname, teknologi.name(), e.getMessage());
            throw new DatabaseDependencyException("Error while registering logs received for database " + dbname + " and teknologi " + teknologi.name(), e);
        }
    }
}
