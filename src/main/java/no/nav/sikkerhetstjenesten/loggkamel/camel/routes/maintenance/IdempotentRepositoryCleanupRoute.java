package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.maintenance;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class IdempotentRepositoryCleanupRoute extends RouteBuilder {

    public static final String CLEANUP_ROUTE_ID = "idempotent-repository-cleanup";

    @Override
    public void configure() {
//        from("quartz:" + CLEANUP_ROUTE_ID + "?cron=0+0+*+*+*+?") // every hour, on the hour
        from("quartz:" + CLEANUP_ROUTE_ID + "?cron=0+*+*+*+*+?") // every minute, on the minute
            .routeId(CLEANUP_ROUTE_ID)
            .log(LoggingLevel.INFO, "Running cleanup of expired idempotent repository entries")
//            .setBody(constant("DELETE FROM CAMEL_MESSAGEPROCESSED WHERE createdAt < NOW() - INTERVAL '1 hour'"))
            .setBody(constant("DELETE FROM CAMEL_MESSAGEPROCESSED WHERE createdAt < NOW() - INTERVAL '1 minute'"))
            .to("spring-jdbc:default")
            .log(LoggingLevel.INFO, "Idempotent repository cleanup complete");
    }
}
