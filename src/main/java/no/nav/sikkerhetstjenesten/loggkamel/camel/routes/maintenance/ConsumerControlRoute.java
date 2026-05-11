package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.maintenance;

import io.getunleash.Unleash;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.LogLineMessageConsumer.LOG_LINE_MESSAGE_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogGroupConsumer.POSTGRES_LOG_CONSUMER_ID;

@Component
public class ConsumerControlRoute extends RouteBuilder {

    @Autowired
    private Unleash unleash;

    public static final String CONSUMER_CONTROL_ROUTE_ID = "consumer-control-route";

    @Override
    public void configure() {
        from("quartz:" + CONSUMER_CONTROL_ROUTE_ID + "?cron=0+*+*+*+*+?") // Every 10 minutes
                .routeId(CONSUMER_CONTROL_ROUTE_ID)
                .log(LoggingLevel.INFO, "Checking whether to disable consumer routes based on feature flags")
                .process(exchange -> {
                    boolean consumePostgresLogs = unleash.isEnabled("consume-postgres-logs", false);
                    if (consumePostgresLogs && exchange.getContext().getRouteController().getRouteStatus(POSTGRES_LOG_CONSUMER_ID).isStopped()) {
                        log.info("Feature flag 'consume-postgres-logs' is enabled, starting route {}", POSTGRES_LOG_CONSUMER_ID);
                        exchange.getContext().getRouteController().startRoute(POSTGRES_LOG_CONSUMER_ID);
                    } else if (!consumePostgresLogs && exchange.getContext().getRouteController().getRouteStatus(POSTGRES_LOG_CONSUMER_ID).isStarted()) {
                        log.info("Feature flag 'consume-postgres-logs' is disabled, stopping route {}", POSTGRES_LOG_CONSUMER_ID);
                        exchange.getContext().getRouteController().stopRoute(POSTGRES_LOG_CONSUMER_ID);
                    }
                })
                .process(exchange -> {
                    boolean consumeLogLines = unleash.isEnabled("consume-log-lines", false);
                    if (consumeLogLines && exchange.getContext().getRouteController().getRouteStatus(LOG_LINE_MESSAGE_CONSUMER_ID).isStopped()) {
                        log.info("Feature flag 'consume-log-lines' is enabled, starting route {}", LOG_LINE_MESSAGE_CONSUMER_ID);
                        exchange.getContext().getRouteController().startRoute(LOG_LINE_MESSAGE_CONSUMER_ID);
                    } else if (!consumeLogLines && exchange.getContext().getRouteController().getRouteStatus(LOG_LINE_MESSAGE_CONSUMER_ID).isStarted()) {
                        log.info("Feature flag 'consume-log-lines' is disabled, stopping route {}", LOG_LINE_MESSAGE_CONSUMER_ID);
                        exchange.getContext().getRouteController().stopRoute(LOG_LINE_MESSAGE_CONSUMER_ID);
                    }
                })
                .log(LoggingLevel.INFO, "Consumer route control check complete");
    }
}
