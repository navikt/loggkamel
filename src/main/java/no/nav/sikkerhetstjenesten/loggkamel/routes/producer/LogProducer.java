package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;

public abstract class LogProducer extends RouteBuilder {

    @Value("${routing.postgres.producer}")
    String producerUri;

    public static String POSTGRES_LOG_PRODUCER_ID = "postgres-log-producer";
    public static String POSTGRES_LOG_PRODUCER_ROUTE = "direct:" + POSTGRES_LOG_PRODUCER_ID;
}
