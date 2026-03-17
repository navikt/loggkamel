package no.nav.sikkerhetstjenesten.loggkamel.routes;

import no.nav.sikkerhetstjenesten.loggkamel.processor.InvalidIndividualPostgresLog;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;

public abstract class SharedRouteErrorHandler extends RouteBuilder {

    @Value("${routing.postgres.dead-letter}")
    protected String deadLetterUri;

    @Value("${routing.postgres.invalid-message}")
    protected String invalidMessageUri;

    public abstract void configure();

    public void errorHandling() {
        errorHandler(deadLetterChannel(deadLetterUri)
                .useOriginalMessage()
                .maximumRedeliveries(1)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.INFO)
                .retriesExhaustedLogLevel(LoggingLevel.WARN)
                .logExhaustedMessageHistory(true)
                .onPrepareFailure(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String fileName = exchange.getIn().getHeader(org.apache.camel.Exchange.FILE_NAME, String.class);
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

        onException(InvalidIndividualPostgresLog.class)
                .handled(true)
                .useOriginalMessage()
                .log("Routing invalid message to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);
    }
}
