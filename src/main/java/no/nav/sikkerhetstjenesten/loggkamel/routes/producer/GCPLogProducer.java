package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.boot.conditionals.ConditionalOnGCP;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.processor.PostgresLogEnrichmentProcessor.LOG_VALUES;

@Component
@ConditionalOnGCP
public class GCPLogProducer extends LogProducer {

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                .process(exchange -> {
                    try (Logging logging = LoggingOptions.getDefaultInstance().getService()) {
                        Payload.JsonPayload jsonPayload = Payload.JsonPayload.of((Map<String, ?>) exchange.getVariables().get(LOG_VALUES));

                        LogEntry entry =
                                LogEntry.newBuilder(jsonPayload)
                                        .setSeverity(Severity.ERROR)
                                        .setLogName("loggkamel-backup")
                                        .build();

                        logging.write(Collections.singleton(entry));
                    }
                });
    }
}
