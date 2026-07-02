package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.maintenance;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.LOG_LINE_CONSUMER;
import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.POSTGRES_CONSUMER;

@Component
public class IdempotentRepositoryCleanupRoute extends RouteBuilder {

    private static final String CLEANUP_ROUTE_ROOT = "idempotent-repository-cleanup";
    private static final String POSTGRES_CLEANUP_ROUTE_ID = "postgres-" + CLEANUP_ROUTE_ROOT;
    private static final String LOG_LINE_CLEANUP_ROUTE_ID = "logline-" + CLEANUP_ROUTE_ROOT;

    @Override
    public void configure() {
        from("quartz:" + POSTGRES_CLEANUP_ROUTE_ID + "?cron=0+0/5+*+*+*+?") // every five minutes
            .routeId(POSTGRES_CLEANUP_ROUTE_ID)
            .log(LoggingLevel.DEBUG, "Running cleanup of expired postgres idempotent repository entries")
            .setBody(constant("DELETE FROM CAMEL_MESSAGEPROCESSED WHERE processorname = '" + POSTGRES_CONSUMER + "' and createdAt < NOW() - INTERVAL '6 hours'"))
            .to("spring-jdbc:default")
            .log(LoggingLevel.DEBUG, "Idempotent repository cleanup complete");

        from("quartz:" + LOG_LINE_CLEANUP_ROUTE_ID + "?cron=0+0/5+*+*+*+?") // every five minutes
                .routeId(LOG_LINE_CLEANUP_ROUTE_ID)
                .log(LoggingLevel.DEBUG, "Running cleanup of expired log line idempotent repository entries")
                .setBody(constant("DELETE FROM CAMEL_MESSAGEPROCESSED WHERE processorname = '" + LOG_LINE_CONSUMER + "' and createdAt < NOW() - INTERVAL '10 minutes'"))
                .to("spring-jdbc:default")
                .log(LoggingLevel.DEBUG, "Idempotent repository cleanup complete");
    }
}
