package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggLineErrorHandler;

public abstract class ArkivLoggProducer extends LoggLineErrorHandler {

    public static String ARKIVLOGG_PRODUCER_ID = "arkivlogg-producer";
    public static String ARKIVLOGG_PRODUCER_ROUTE = "direct:" + ARKIVLOGG_PRODUCER_ID;
}
