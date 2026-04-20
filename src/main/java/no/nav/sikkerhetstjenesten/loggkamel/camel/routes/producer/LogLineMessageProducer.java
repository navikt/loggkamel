package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogLineMessageProducer extends SharedRouteErrorHandler {

    @Value("${routing.loggline.bucket}")
    String logLineMessageBucketUri;

    public static String LOG_LINE_MESSAGE_PRODUCER = "loggline-producer";
    public static String LOG_LINE_MESSAGE_PRODUCER_ROUTE = "direct:" + LOG_LINE_MESSAGE_PRODUCER;


    @Override
    public void configure() {
        super.errorHandling();

        //TODO: update filename to make clear that you are producing a log line message, not the original file
        from(LOG_LINE_MESSAGE_PRODUCER_ROUTE)
                .routeId(LOG_LINE_MESSAGE_PRODUCER)
                .log("Producing loggline message ${header.CamelFileName} to log line endpoint")
                .process(exchange -> exchange.getMessage().setBody(objectMapper.writeValueAsString(exchange.getMessage().getBody())))
                .toD(logLineMessageBucketUri);
    }
}
