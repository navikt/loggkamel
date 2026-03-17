package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@ConditionalOnLocalOrTest
public class LocalLogProducer extends LogProducer {

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                //TODO: refine this from all message variables to just those we'd want to persist in log API
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getMessage().getBody() + ", messageVariables: " + exchange.getVariables());
                })
                // INSERT GCP LOGS HERE FOR TESTING
                .process(exchange -> {
                    try (Logging logging = LoggingOptions.getDefaultInstance().getService()) {
                        LogEntry entry =
                                LogEntry.newBuilder(Payload.StringPayload.of(exchange.getIn().getBody(String.class)))
                                        .setSeverity(Severity.ERROR)
                                        .setLogName("my-log") //TODO: select more appropriate name
                                        .build();

                        // Writes the log entry asynchronously
                        logging.write(Collections.singleton(entry));
                    } catch (Exception e) {
                        // TODO: expect and handle GCP connectivity or configuration issues separately
                        log.error("Failed to publish to GCP, exception: {}", e.getMessage());
                        throw e;
                    }
                })
                .toD(producerUri);
    }
}
