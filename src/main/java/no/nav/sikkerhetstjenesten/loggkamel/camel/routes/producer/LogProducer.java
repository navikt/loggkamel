package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;

public abstract class LogProducer extends SharedRouteErrorHandler {

    public static String POSTGRES_LOG_PRODUCER_ID = "postgres-log-producer";
    public static String POSTGRES_LOG_PRODUCER_ROUTE = "direct:" + POSTGRES_LOG_PRODUCER_ID;
}
