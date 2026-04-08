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

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnricher.LOG_ENRICHMENT;

@Component
@ConditionalOnGCP
public class GCPLogProducer extends LogProducer {

    @Autowired
    ObjectMapper mapper;

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                .log("Producing log message ${header.CamelFileName} to GCP Logging")
                .process(exchange -> {
                    try (Logging logging = LoggingOptions.getDefaultInstance().getService()) {

                        Map<String, Object> logEnrichmentMap = mapper.convertValue(exchange.getVariables().get(LOG_ENRICHMENT), new TypeReference<>() {});
                        Payload.JsonPayload jsonPayload = Payload.JsonPayload.of(logEnrichmentMap);

                        LogEntry entry =
                                LogEntry.newBuilder(jsonPayload)
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
