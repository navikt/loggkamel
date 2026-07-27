package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance;

import com.google.common.collect.ImmutableList;
import io.getunleash.Unleash;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
public class FeatureFlagControlRouteProcessor {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagControlRouteProcessor.class);

    static final String CONSUME_POSTGRES_STREAMS_FEATURE_FLAG = "consume-postgres-logs";
    static final String CONSUME_LOG_PACKETS_FEATURE_FLAG = "consume-log-lines";
    static final String PUBLISH_LOG_LINES_FEATURE_FLAG = "publish-log-lines";

    @AllArgsConstructor
    @Getter
    class RouteConfiguration {
        private String featureFlag;
        private String routeId;
        private Boolean defaultState;
    }

    final ImmutableList<RouteConfiguration> ROUTE_CONFIGURATIONS = ImmutableList.of(
            new RouteConfiguration(CONSUME_POSTGRES_STREAMS_FEATURE_FLAG, POSTGRES_LOG_CONSUMER_ID, false),
            new RouteConfiguration(CONSUME_LOG_PACKETS_FEATURE_FLAG, NATIVE_LOG_PACKET_CONSUMER_ID, false),
            new RouteConfiguration(PUBLISH_LOG_LINES_FEATURE_FLAG, STANDARDIZED_LOG_LINE_PRODUCER_ID, true)
    );

    @Autowired
    private Unleash unleash;

    public void updateAllRoutes(Exchange exchange) throws Exception {
        for (RouteConfiguration routeConfig : ROUTE_CONFIGURATIONS) {
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
