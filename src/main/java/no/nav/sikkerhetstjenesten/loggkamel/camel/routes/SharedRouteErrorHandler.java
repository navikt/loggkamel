package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import no.nav.sikkerhetstjenesten.loggkamel.camel.InvalidLogLineException;
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

                    log.warn(
                            "Messaged failed to process normally. routeId={}, fileName={}, exceptionType={}, reason={}",
                            routeId,
                            fileName,
                            exceptionType,
                            exceptionMessage,
                            cause
                    );
                })
        );

        onException(InvalidLogLineException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalMessage()
                .log("Routing InvalidIndividualPostgresLog to dead-letter after retries: ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(deadLetterUri);

        onException(Exception.class)
                .maximumRedeliveries(0)
                .handled(true)
                .useOriginalMessage()
                .log("Routing exception directly to invalid-messages channel: ${exception.class} - ${exception.message}, filename: ${headers['CamelFileName']}")
                .to(invalidMessageUri);
    }
}
