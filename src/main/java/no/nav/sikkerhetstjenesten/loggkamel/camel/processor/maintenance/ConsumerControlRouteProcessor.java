package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance;

import io.getunleash.Unleash;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.NativeLogPacketConsumer.NATIVE_LOG_PACKET_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogStreamConsumer.POSTGRES_LOG_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.StandardizedLogLineProducer.STANDARDIZED_LOG_LINE_PRODUCER_ID;

@Service
public class ConsumerControlRouteProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConsumerControlRouteProcessor.class);

    static final String CONSUME_POSTGRES_STREAMS_FEATURE_FLAG = "consume-postgres-logs";
    static final String CONSUME_LOG_PACKETS_FEATURE_FLAG = "consume-log-lines";
    static final String PUBLIC_LOG_LINES_FEATURE_FLAG = "publish-log-lines";

    static final Map<String, String> FEATURE_FLAGS_TO_CONTROLLED_ROUTES = Map.of(
            CONSUME_POSTGRES_STREAMS_FEATURE_FLAG, POSTGRES_LOG_CONSUMER_ID,
            CONSUME_LOG_PACKETS_FEATURE_FLAG, NATIVE_LOG_PACKET_CONSUMER_ID,
            PUBLIC_LOG_LINES_FEATURE_FLAG, STANDARDIZED_LOG_LINE_PRODUCER_ID
            );

    @Autowired
    private Unleash unleash;

    public void updateAllRoutes(Exchange exchange) throws Exception {
        for (String featureFlag : FEATURE_FLAGS_TO_CONTROLLED_ROUTES.keySet()) {
            updateRouteStatus(exchange, featureFlag);
        }
    }

    private void updateRouteStatus(Exchange exchange, String featureFlagForRoute) throws Exception {
        boolean routeShouldBeRunning = unleash.isEnabled(featureFlagForRoute, false);
        String routeIdBeingControlled = FEATURE_FLAGS_TO_CONTROLLED_ROUTES.get(featureFlagForRoute);
        boolean routeIsRunning = exchange.getContext().getRouteController().getRouteStatus(routeIdBeingControlled).isStarted();
        if (routeShouldBeRunning && !routeIsRunning) {
            log.info("Feature flag '{}' is enabled, starting route {}", featureFlagForRoute, routeIdBeingControlled);
            exchange.getContext().getRouteController().startRoute(routeIdBeingControlled);
        } else if (!routeShouldBeRunning && exchange.getContext().getRouteController().getRouteStatus(POSTGRES_LOG_CONSUMER_ID).isStarted()) {
            log.info("Feature flag '{}' is disabled, stopping route {}", featureFlagForRoute, routeIdBeingControlled);
            exchange.getContext().getRouteController().stopRoute(routeIdBeingControlled);
        }
    }
}
