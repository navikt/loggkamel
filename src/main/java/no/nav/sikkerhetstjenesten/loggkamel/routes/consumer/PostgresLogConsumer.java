package no.nav.sikkerhetstjenesten.loggkamel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.processor.InvalidIndividualPostgresLog;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.PostgresLogEnricher.POSTGRES_LOG_ENRICH_ROUTE;

@Component
public class PostgresLogConsumer extends RouteBuilder {

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.dead-letter}")
    private String deadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    private String invalidMessageUri;

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Override
    public void configure() {
        //TODO: try to set up such that dead letter only handles postgres messages, not all undelivered messages
        // Alternatively, pull out of here and into a universal route definition

        //TODO: decide whether to re-compress failed messages, or leave uncompressed
        errorHandler(deadLetterChannel(deadLetterUri)
//                .useOriginalMessage()
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

        //TODO: should narrow exception class to keep this specific to postgres?
        //TODO: test that this log message formatting works as expected
        // Invalid logical message format
        onException(InvalidIndividualPostgresLog.class)
                .handled(true)
                .useOriginalMessage()
                .log("Routing invalid message to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);

        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")

        from(consumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .process(exchange -> {
                    // If the file comes from a bucket instead of local storage, still populate the filename
                    if (exchange.getIn().getHeader("CamelFileName", String.class) == null) {
                        exchange.getIn().setHeader("CamelFileName", exchange.getIn().getHeader("CamelGoogleCloudStorageObjectName", String.class));
                    }
                })
                .choice()
                .when(header("CamelFileName").endsWith(".gz"))
                    .doTry()
                        .unmarshal().gzipDeflater()
                    .endDoTry()
                    .doCatch(IOException.class)
                        .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                        .to(invalidMessageUri)
                        .stop()
                    .end()
                .end()
                // Split the log file by lines, and strip the leading "<" symbols
                .split(body().tokenize("^\\<|\n\\<")).streaming()
                .process(exchange -> {
                    String originalFileName = exchange.getIn().getHeader("CamelFileName", String.class);
                    // If a line was part of a compressed file, strip compression extension and make unique with a UUID
                    if (originalFileName.endsWith(".gz")) {
                        String newFileName = UUID.randomUUID() + "." + originalFileName.substring(0, originalFileName.length() - 3);
                        exchange.getIn().setHeader("CamelFileName", newFileName);
                    }
                })
                .to(POSTGRES_LOG_ENRICH_ROUTE);
    }
}
