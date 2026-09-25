package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.builder.RouteBuilder;

public abstract class StandardizedLogLineProducer extends RouteBuilder {

    public static final String STANDARDIZED_LOG_LINE_PRODUCER_ID = "standardized-log-line-producer";
    public static final String STANDARDIZED_LOG_LINE_PRODUCER_ROUTE = "direct:" + STANDARDIZED_LOG_LINE_PRODUCER_ID + "?failIfNoConsumers=false&timeout=10&block=false";

    protected final RouteConfigurationIdResolver routeConfigurationIdResolver;

    protected StandardizedLogLineProducer(RouteConfigurationIdResolver routeConfigurationIdResolver) {
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }
}
