package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance;

import io.getunleash.Unleash;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ServiceStatus;
import org.apache.camel.spi.RouteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogStreamConsumer.POSTGRES_LOG_CONSUMER_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagControlRouteProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    CamelContext  camelContext;

    @Mock
    RouteController  routeController;

    @Mock
    ServiceStatus serviceStatus;

    @Mock
    Unleash unleash;

    @InjectMocks
    FeatureFlagControlRouteProcessor processor;

    @BeforeEach
    void setUp() {
        lenient().when(exchange.getContext()).thenReturn(camelContext);
        lenient().when(camelContext.getRouteController()).thenReturn(routeController);
        lenient().when(routeController.getRouteStatus(anyString())).thenReturn(serviceStatus);
    }

    @Test
    void updateAllRoutes_exceptionsPassThrough() {
        String originalExceptionMessage = "slkfdjlkjsadf";
        when(unleash.isEnabled(anyString(), eq(false))).thenThrow(new RuntimeException(originalExceptionMessage));

        assertThrowsExactly(RuntimeException.class, () -> processor.updateAllRoutes(exchange), originalExceptionMessage);
    }

    @Test
    void updateAllRoutes_stoppedRouteIsStarted() throws Exception {
        for (FeatureFlagControlRouteProcessor.RouteConfiguration routeConfig : FeatureFlagControlRouteProcessor.RouteConfiguration.values()) {
            when(unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState())).thenReturn(true);
        }
        when(serviceStatus.isStarted()).thenReturn(false);

        processor.updateAllRoutes(exchange);

        verify(routeController).startRoute(POSTGRES_LOG_CONSUMER_ID);
    }

    @Test
    void updateAllRoutes_runningRouteIsStopped() throws Exception {
        for (FeatureFlagControlRouteProcessor.RouteConfiguration routeConfig : FeatureFlagControlRouteProcessor.RouteConfiguration.values()) {
            when(unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState())).thenReturn(false);
        }
        when(serviceStatus.isStarted()).thenReturn(true);

        processor.updateAllRoutes(exchange);

        verify(routeController).stopRoute(POSTGRES_LOG_CONSUMER_ID);
    }

    @Test
    void updateAllRoutes_routeInCorrectStatusNotModified() throws Exception {
        for (FeatureFlagControlRouteProcessor.RouteConfiguration routeConfig : FeatureFlagControlRouteProcessor.RouteConfiguration.values()) {
            when(unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState())).thenReturn(true);
        }
        when(serviceStatus.isStarted()).thenReturn(true);

        processor.updateAllRoutes(exchange);

        verify(routeController, times(0)).startRoute(anyString());
        verify(routeController, times(0)).stopRoute(anyString());
    }

    @Test
    void updateAllRoutes_allRoutesUpdated() throws Exception {
        for (FeatureFlagControlRouteProcessor.RouteConfiguration routeConfig : FeatureFlagControlRouteProcessor.RouteConfiguration.values()) {
            when(unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState())).thenReturn(true);
        }
        when(serviceStatus.isStarted()).thenReturn(false);

        processor.updateAllRoutes(exchange);

        for (FeatureFlagControlRouteProcessor.RouteConfiguration routeConfig : FeatureFlagControlRouteProcessor.RouteConfiguration.values()) {
            verify(routeController).startRoute(routeConfig.getRouteId());
        }
    }

}