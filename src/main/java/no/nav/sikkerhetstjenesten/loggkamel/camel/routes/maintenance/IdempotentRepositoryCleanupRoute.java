package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.maintenance;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class IdempotentRepositoryCleanupRoute extends RouteBuilder {

    public static final String CLEANUP_ROUTE_ID = "idempotent-repository-cleanup";

    @Override
    public void configure() {
        from("quartz:" + CLEANUP_ROUTE_ID + "?cron=0+0/5+*+*+*+?") // every five minutes
            .routeId(CLEANUP_ROUTE_ID)
            .log(LoggingLevel.DEBUG, "Running cleanup of expired idempotent repository entries")
            .setBody(constant("DELETE FROM CAMEL_MESSAGEPROCESSED WHERE createdAt < NOW() - INTERVAL '2 hours'"))
            .to("spring-jdbc:default")
            .log(LoggingLevel.DEBUG, "Idempotent repository cleanup complete");
    }
}
