package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.TEKNOLOGI;
import static org.apache.camel.Exchange.FILE_NAME;

public abstract class LoggGroupErrorHandler extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(LoggGroupErrorHandler.class);

    @Value("${routing.postgres.dead-letter}")
    protected String postgresDeadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    protected String postgresInvalidMessageUri;

    @Value("${routing.fallback.invalid-message}")
    protected String fallbackInvalidMessageUri;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Metrics metrics;

    public abstract void configure();

    // HERE FOR DEBUG, REMOVE
    /**
     * Utility method to capture and log body state for diagnostics
     */
    protected void logBodyStateForDiagnostics(Exchange exchange, String eventName) {
        try {
            Object body = exchange.getIn().getBody();
            String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);

            String bodyType = body != null ? body.getClass().getSimpleName() : "null";
            long bodySize = 0;
            String bodyHash = "N/A";

            if (body instanceof String) {
                bodySize = ((String) body).length();
                bodyHash = computeHash(((String) body).getBytes());
            } else if (body instanceof byte[]) {
                bodySize = ((byte[]) body).length;
                bodyHash = computeHash((byte[]) body);
            } else if (body instanceof InputStream) {
                bodyType = "InputStream - NOT FULLY BUFFERED";
                bodySize = -1;
                bodyHash = "STREAM_NOT_BUFFERED";
            }

            log.warn("[DIAGNOSTIC-ERROR-HANDLER-{}] File: {}, Exception: {}, Body Type: {}, Body Size: {} bytes, Body Hash: {}",
                    eventName, fileName,
                    exchange.getProperty(Exchange.EXCEPTION_CAUGHT) != null ?
                        exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getClass().getSimpleName() : "NONE",
                    bodyType, bodySize, bodyHash);

        } catch (Exception e) {
            log.warn("[DIAGNOSTIC] Error logging body state: {}", e.getMessage(), e);
        }
    }

    //HERE FOR DEBUG, REMOVE
    /**
     * Compute MD5 hash of byte array for comparison
     */
    protected String computeHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.warn("[DIAGNOSTIC] Error computing hash: {}", e.getMessage());
            return "ERROR_COMPUTING_HASH";
        }
    }

    public void errorHandling() {
        // Allows use of original message in exception handlers for cases where the message is an InputStream, as happens with GCP buckets
        getContext().setStreamCaching(true);
        getContext().setAllowUseOriginalMessage(true);

        // TODO: fix filename reading for messages from GCP, as they do not have CamelFileName initialized
        onException(DependencyException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log("Routing DependencyException to postgres dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .process(exchange -> {
                    log.warn("[DIAGNOSTIC-ERROR] DependencyException caught for file: {}",
                        exchange.getIn().getHeader(FILE_NAME, String.class));
                    logBodyStateForDiagnostics(exchange, "DEPENDENCY_EXCEPTION_BEFORE_ROUTING");
                })
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) //10-second delay between retries
                .handled(true)
                .process(exchange -> {
                    metrics.logsPostgresDeadletter.increment();
                    logBodyStateForDiagnostics(exchange, "DEPENDENCY_EXCEPTION_TO_DEADLETTER");
                })
                .to(postgresDeadLetterUri);

        // Other teknologi-specific dead letter queues go here

        onException(InvalidLogException.class).onWhen(variable(TEKNOLOGI).convertTo(TeknologiEnum.class).isEqualTo(TeknologiEnum.POSTGRESQL))
                .log("Routing InvalidLogException to postgres invalid-messages channel: ${exception.message}, filename: ${headers['CamelFileName']}")
                .useOriginalBody()
                .process(exchange -> {
                    log.warn("[DIAGNOSTIC-ERROR] InvalidLogException caught for file: {}",
                        exchange.getIn().getHeader(FILE_NAME, String.class));
                    logBodyStateForDiagnostics(exchange, "INVALID_LOG_EXCEPTION_BEFORE_ROUTING");
                })
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    metrics.logsPostgresInvalid.increment();
                    logBodyStateForDiagnostics(exchange, "INVALID_LOG_EXCEPTION_TO_QUEUE");
                })
                .to(postgresInvalidMessageUri);

        // Other teknologi-specific invalid message queues go here

        onException(Exception.class)
                .log(LoggingLevel.WARN, "Routing unhandled exception to fallback invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .log(LoggingLevel.DEBUG, "Exception stack trace: ${exception.stacktrace}")
                .useOriginalBody()
                .process(exchange -> {
                    log.warn("[DIAGNOSTIC-ERROR] Generic Exception caught for file: {}",
                        exchange.getIn().getHeader(FILE_NAME, String.class));
                    logBodyStateForDiagnostics(exchange, "GENERIC_EXCEPTION_BEFORE_ROUTING");
                })
                .maximumRedeliveries(0)
                .handled(true)
                .process(exchange -> {
                    metrics.logsFallbackInvalid.increment();
                    logBodyStateForDiagnostics(exchange, "GENERIC_EXCEPTION_TO_QUEUE");
                })
                .to(fallbackInvalidMessageUri);
    }
}
