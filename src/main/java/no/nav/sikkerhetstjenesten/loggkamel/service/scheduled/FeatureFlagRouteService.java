package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import io.getunleash.Unleash;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.RouteController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.NativeLogPacketConsumer.NATIVE_LOG_PACKET_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogStreamConsumer.POSTGRES_LOG_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.StandardizedLogLineProducer.STANDARDIZED_LOG_LINE_PRODUCER_ID;

@Service
public class FeatureFlagRouteService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagRouteService.class);

    @AllArgsConstructor
    @Getter
    enum RouteConfiguration {
        POSTGRES_LOGS("consume-postgres-logs", POSTGRES_LOG_CONSUMER_ID, false),
        LOG_PACKETS("consume-log-lines", NATIVE_LOG_PACKET_CONSUMER_ID, false);

        private final String featureFlag;
        private final String routeId;
        private final Boolean defaultState;
    }

    private final CamelContext camelContext;
    private final Unleash unleash;

    @Autowired
    public FeatureFlagRouteService(CamelContext camelContext,
                                   Unleash unleash) {
        this.camelContext = camelContext;
        this.unleash = unleash;
    }

    @Scheduled(cron = "${scheduled.route.control.cron}", zone = "${app.timezone}")
    public void updateAllRoutes() {
        for (RouteConfiguration routeConfig : RouteConfiguration.values()) {
            updateRouteStatus(routeConfig);
        }
    }

    private void updateRouteStatus(RouteConfiguration routeConfig) {
        boolean routeShouldBeRunning = unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState());
        String routeIdBeingControlled = routeConfig.getRouteId();
        RouteController routeController = camelContext.getRouteController();
        boolean routeIsRunning = routeController.getRouteStatus(routeIdBeingControlled).isStarted();

        try {
            if (routeShouldBeRunning && !routeIsRunning) {
                log.info("Feature flag '{}' is enabled, starting route {}", routeConfig.getFeatureFlag(), routeIdBeingControlled);
                routeController.startRoute(routeIdBeingControlled);
            } else if (!routeShouldBeRunning && routeIsRunning) {
                log.info("Feature flag '{}' is disabled, stopping route {}", routeConfig.getFeatureFlag(), routeIdBeingControlled);
                routeController.stopRoute(routeIdBeingControlled);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update route " + routeIdBeingControlled, e);
        }
    }
}
