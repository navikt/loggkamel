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
        // Delivery / technical failures for otherwise valid messages
        errorHandler(deadLetterChannel(deadLetterUri)
//                .useOriginalMessage()
                .maximumRedeliveries(1)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
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
        // Invalid logical message format
        onException(InvalidIndividualPostgresLog.class)
                .handled(true)
                .useOriginalMessage()
                .log("Routing invalid message to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}") //TODO: add invalid file name here
                //TODO: encode/marshall as .gz file here
                .to(invalidMessageUri);

        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")

        from(consumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                // TODO: only unmarshall if file is encoded
                .doTry()
                    .unmarshal().gzipDeflater()
                .endDoTry()
                .doCatch(IOException.class)
                    .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                    .to(invalidMessageUri)
                    .stop()
                .end()
                .split(body().tokenize("^\\<|\n\\<")).streaming()
                //TODO: only remove the .gz ending if it's present, only add the UUID if the .gz ending was present
                .process(exchange -> {
                    // TODO: added this for debugging, remove
                    log.info("Message headers: {}", exchange.getIn().getHeaders());

                    String originalFileName = exchange.getIn().getHeader("CamelFileName", String.class);
                    String newFileName = UUID.randomUUID() + "." + originalFileName.substring(0, originalFileName.length() - 3);
                    exchange.getIn().setHeader("CamelFileName", newFileName);
                })
                .to(POSTGRES_LOG_ENRICH_ROUTE);
    }
}
