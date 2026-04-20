package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.GCPDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEAM_GCP_PROJECT_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor.LOG_ENRICHMENT;

@Component
@ConditionalOnGCP
public class GCPArkivLoggProducer extends ArkivLoggProducer {

    @Autowired
    ObjectMapper mapper;

    @Override
    public void configure() {
        super.errorHandling();

        from(ARKIVLOGG_PRODUCER_ROUTE)
                .routeId(ARKIVLOGG_PRODUCER_ID)
                .log("Producing log message ${header.CamelFileName} to GCP Logging")
                .process(exchange -> {
                    String targetGCPProjectId = exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class);

                    try (Logging logging = LoggingOptions.newBuilder()
                            .setProjectId(targetGCPProjectId)
                            .build()
                            .getService()) {

                        Map<String, Object> logMessageAsMap = mapper.convertValue(exchange.getVariables().get(LOG_ENRICHMENT), new TypeReference<>() {});
                        Payload.JsonPayload logMessageAsJsonPayload = Payload.JsonPayload.of(logMessageAsMap);

                        LogEntry entry =
                                LogEntry.newBuilder(logMessageAsJsonPayload)
                                        .setSeverity(Severity.INFO)
                                        .setLogName("loggkamel-arkiv")
                                        .build();

                        logging.write(Collections.singleton(entry));
                    } catch (Exception e) {
                        log.warn("Error while writing log entry to GCP Logging for file {}, error message: {}", exchange.getMessage().getHeader("CamelFileName", String.class), e.getMessage());
                        throw new GCPDependencyException("Error while writing log entry to GCP Logging for file " + exchange.getMessage().getHeader("CamelFileName", String.class), e);
                    }
                });
    }
}
