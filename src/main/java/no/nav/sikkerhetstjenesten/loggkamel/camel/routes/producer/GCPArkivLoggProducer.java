package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;

@Component
@ConditionalOnGCP
public class GCPArkivLoggProducer extends ArkivLoggProducer {

    @Autowired
    private Metrics metrics;

    @Override
    public void configure() {
        super.errorHandling();

        from(ARKIVLOGG_PRODUCER_ROUTE)
            .routeId(ARKIVLOGG_PRODUCER_ID)
            .log("Producing log message ${header.CamelFileName} to GCP Logging")
            .process(exchange -> {
                metrics.incrementHappyPath(Metrics.Multiplicity.single,  exchange.getVariable(TEKNOLOGI, String.class).toLowerCase(), Metrics.Action.produced);
                String dbName = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class).getDbname();
                metrics.incrementDatabaseSpecificAction(dbName,  exchange.getVariable(TEKNOLOGI, String.class).toLowerCase(), Metrics.Action.produced);
            })
            .process(exchange -> {
                String targetGCPProjectId = exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class);

                try (Logging logging = LoggingOptions.newBuilder()
                        .setProjectId(targetGCPProjectId)
                        .build()
                        .getService()) {

                    EnrichedAuditlogg logMessageAsPOJO = exchange.getMessage().getBody(EnrichedAuditlogg.class);
                    Map<String, Object> logMessageAsMap = objectMapper.convertValue(logMessageAsPOJO, new TypeReference<>() {});
                    Payload.JsonPayload logMessageAsJsonPayload = Payload.JsonPayload.of(logMessageAsMap);

                    String teknologiLabel = exchange.getVariable(TEKNOLOGI, TeknologiEnum.class) == TeknologiEnum.POSTGRESQL ? "FSS-" + TeknologiEnum.POSTGRESQL.name() : exchange.getVariable(TEKNOLOGI, String.class);

                    LogEntry entry =
                            LogEntry.newBuilder(logMessageAsJsonPayload)
                                    .setSeverity(Severity.INFO)
                                    .setLogName("loggkamel-arkiv")
                                    .setResource(MonitoredResource.newBuilder(teknologiLabel)
                                            .addLabel("dbname", logMessageAsPOJO.getDbName())
                                            .build())
                                    .build();

                    logging.write(Collections.singleton(entry));
                } catch (Exception e) {
                    log.warn("Error while writing log entry to GCP Logging for file {}, error message: {}", exchange.getMessage().getHeader("CamelFileName", String.class), e.getMessage());
                    throw new GCPDependencyException("Error while writing log entry to GCP Logging for file " + exchange.getMessage().getHeader("CamelFileName", String.class), e);
                }
            });
    }
}
