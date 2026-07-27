package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance;

import io.getunleash.Unleash;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.NativeLogPacketConsumer.NATIVE_LOG_PACKET_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogStreamConsumer.POSTGRES_LOG_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.StandardizedLogLineProducer.STANDARDIZED_LOG_LINE_PRODUCER_ID;

@Service
public class FeatureFlagControlRouteProcessor {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagControlRouteProcessor.class);

    @AllArgsConstructor
    @Getter
    enum RouteConfiguration {
        POSTGRES_LOGS("consume-postgres-logs", POSTGRES_LOG_CONSUMER_ID, false),
        LOG_PACKETS("consume-log-lines", NATIVE_LOG_PACKET_CONSUMER_ID, false),
        LOG_LINES("publish-log-lines", STANDARDIZED_LOG_LINE_PRODUCER_ID, true);

        private final String featureFlag;
        private final String routeId;
        private final Boolean defaultState;
    }

    @Autowired
    private Unleash unleash;

    public void updateAllRoutes(Exchange exchange) throws Exception {
        for (RouteConfiguration routeConfig : RouteConfiguration.values()) {
            updateRouteStatus(exchange, routeConfig);
        }
    }

    private void updateRouteStatus(Exchange exchange, RouteConfiguration routeConfig) throws Exception {
        boolean routeShouldBeRunning = unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState());
        String routeIdBeingControlled = routeConfig.getRouteId();
        boolean routeIsRunning = exchange.getContext().getRouteController().getRouteStatus(routeIdBeingControlled).isStarted();
        if (routeShouldBeRunning && !routeIsRunning) {
            log.info("Feature flag '{}' is enabled, starting route {}", routeConfig.getFeatureFlag(), routeIdBeingControlled);
            exchange.getContext().getRouteController().startRoute(routeIdBeingControlled);
        } else if (!routeShouldBeRunning && routeIsRunning) {
            log.info("Feature flag '{}' is disabled, stopping route {}", routeConfig.getFeatureFlag(), routeIdBeingControlled);
            exchange.getContext().getRouteController().stopRoute(routeIdBeingControlled);
        }
    }
}
