package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.maintenance;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance.ConsumerControlRouteProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ConsumerControlRoute extends RouteBuilder {

    public static final String CONSUMER_CONTROL_ROUTE_ID = "consumer-control-route";

    @Override
    public void configure() {
        from("quartz:" + CONSUMER_CONTROL_ROUTE_ID + "?cron=0+*+*+*+*+?") // Check whether to enable every minute
                .routeId(CONSUMER_CONTROL_ROUTE_ID)
                .log(LoggingLevel.DEBUG, "Checking whether to disable consumer routes based on feature flags")
                .bean(ConsumerControlRouteProcessor.class, "updateAllRoutes")
                .log(LoggingLevel.DEBUG, "Consumer route control check complete");
    }
}
