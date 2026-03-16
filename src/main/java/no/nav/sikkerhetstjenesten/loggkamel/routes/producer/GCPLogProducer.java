package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.boot.conditionals.ConditionalOnGCP;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@ConditionalOnGCP
public class GCPLogProducer extends LogProducer {

    @Override
    public void configure() {
        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                // TODO: build new log body that is a json blob containing both original message body and collected metadata
                // TODO: instead of sending to a destination, send to a bean that uploads it to the google log api
                .process(exchange -> {
                    try (Logging logging = LoggingOptions.getDefaultInstance().getService()) {

                        log.info("Attempting to log body: {}", exchange.getIn().getBody(String.class));
                        log.info("Attempting to log name: {}", exchange.getIn().getHeader("CamelFileName", String.class));

                        LogEntry entry =
                                LogEntry.newBuilder(Payload.StringPayload.of(exchange.getIn().getBody(String.class)))
                                        .setSeverity(Severity.ERROR)
                                        .setLogName(exchange.getIn().getHeader("CamelFileName", String.class))
                                        .setResource(MonitoredResource.newBuilder("global").build())
                                        .build();

                        // Writes the log entry asynchronously
                        logging.write(Collections.singleton(entry));

                        // Optional - flush any pending log entries just before Logging is closed
                        logging.flush();
                    } catch (Exception e) {
                        // TODO: expect and handle GCP connectivity or configuration issues separately
                        log.error("Failed to publish to GCP, exception: {}", e.getMessage());
                        throw e;
                    }
                });
    }
}
