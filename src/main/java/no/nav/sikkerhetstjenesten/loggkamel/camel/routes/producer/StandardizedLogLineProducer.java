package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogPacketErrorHandler;

public abstract class StandardizedLogLineProducer extends LogPacketErrorHandler {

    public static final String STANDARDIZED_LOG_LINE_PRODUCER_ID = "standardized-log-line-producer";
    public static final String STANDARDIZED_LOG_LINE_PRODUCER_ROUTE = "direct:" + STANDARDIZED_LOG_LINE_PRODUCER_ID + "?failIfNoConsumers=false&block=true&timeout=61000";
}
