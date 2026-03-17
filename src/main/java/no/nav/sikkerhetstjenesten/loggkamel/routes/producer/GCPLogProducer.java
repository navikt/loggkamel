package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import no.nav.boot.conditionals.ConditionalOnGCP;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@ConditionalOnGCP
public class GCPLogProducer extends LogProducer {

    @Value("${routing.postgres.dead-letter}")
    private String deadLetterUri;

    @Override
    public void configure() {

        //TODO: putting here for troubleshooting to see if errorHandler is route-specific, remove after testing
        errorHandler(deadLetterChannel(deadLetterUri)
                .useOriginalMessage()
                .maximumRedeliveries(1)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.INFO)
                .retriesExhaustedLogLevel(LoggingLevel.WARN)
                .logExhaustedMessageHistory(true)
                .onPrepareFailure(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                    String routeId = exchange.getFromRouteId();

                    String exceptionType = cause != null ? cause.getClass().getName() : "unknown";
                    String exceptionMessage = cause != null ? cause.getMessage() : "unknown";

                    exchange.getIn().setHeader("deadLetterExceptionType", exceptionType);
                    exchange.getIn().setHeader("deadLetterReason", exceptionMessage);
                    exchange.getIn().setHeader("deadLetterRouteId", routeId);
                    exchange.getIn().setHeader("deadLetterFileName", fileName);

                    log.error(
                            "Routing message to dead letter channel. routeId={}, fileName={}, exceptionType={}, reason={}",
                            routeId,
                            fileName,
                            exceptionType,
                            exceptionMessage,
                            cause
                    );
                })
        );


        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                // TODO: build new log body that is a json blob containing both original message body and collected metadata
                // TODO: instead of sending to a destination, send to a bean that uploads it to the google log api
                .process(exchange -> {
                    try (Logging logging = LoggingOptions.getDefaultInstance().getService()) {
                        LogEntry entry =
                                LogEntry.newBuilder(Payload.StringPayload.of(exchange.getIn().getBody(String.class)))
                                        .setSeverity(Severity.ERROR)
                                        .setLogName(exchange.getIn().getHeader("CamelFileName", String.class))
                                        .build();

                        // Writes the log entry asynchronously
                        logging.write(Collections.singleton(entry));
                    } catch (Exception e) {
                        // TODO: expect and handle GCP connectivity or configuration issues separately
                        log.error("Failed to publish to GCP, exception: {}", e.getMessage());
                        throw e;
                    }
                });
    }
}
