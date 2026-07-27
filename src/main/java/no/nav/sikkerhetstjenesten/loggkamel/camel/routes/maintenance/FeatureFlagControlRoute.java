package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.maintenance;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance.FeatureFlagControlRouteProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagControlRoute extends RouteBuilder {

    public static final String FEATURE_FLAG_CONTROL_ROUTE_ID = "feature-flag-control-route";

    @Override
    public void configure() {
        from("quartz:" + FEATURE_FLAG_CONTROL_ROUTE_ID + "?cron=0+*+*+*+*+?") // Check whether to enable every minute
                .routeId(FEATURE_FLAG_CONTROL_ROUTE_ID)
                .log(LoggingLevel.DEBUG, "Checking whether to disable consumer routes based on feature flags")
                .bean(FeatureFlagControlRouteProcessor.class, "updateAllRoutes")
                .log(LoggingLevel.DEBUG, "Consumer route control check complete");
    }
}
