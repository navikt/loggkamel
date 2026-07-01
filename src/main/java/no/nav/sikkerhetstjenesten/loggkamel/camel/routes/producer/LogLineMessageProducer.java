package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.LogLineMessageProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogLineMessageProducer extends LoggGroupErrorHandler {

    @Value("${routing.loggline.bucket}")
    String logLineMessageBucketUri;

    public static String LOG_LINE_MESSAGE_PRODUCER = "loggline-producer";
    public static String LOG_LINE_MESSAGE_PRODUCER_ROUTE = "direct:" + LOG_LINE_MESSAGE_PRODUCER;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_LINE_MESSAGE_PRODUCER_ROUTE)
                .routeId(LOG_LINE_MESSAGE_PRODUCER)
                .log(LoggingLevel.INFO, "Producing loggline message ${header.CamelFileName} to log line endpoint")
                .throwException(new Exception("TESTING FAILURE AFTER SPLITTING")) //TODO: remove after testing
                .bean(LogLineMessageProducerProcessor.class, "incrementMetrics")
                .bean(LogLineMessageProducerProcessor.class, "mapToAuditloggLineMessage")
                .toD(logLineMessageBucketUri);
    }
}
