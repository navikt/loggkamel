package no.nav.sikkerhetstjenesten.loggkamel.service.scheduled;

import io.getunleash.Unleash;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.spi.RouteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogStreamConsumer.POSTGRES_LOG_CONSUMER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagRouteServiceTest {

    @Mock
    CamelContext camelContext;

    @Mock
    RouteController routeController;

    @Mock
    ServiceStatus serviceStatus;

    @Mock
    Unleash unleash;

    @InjectMocks
    FeatureFlagRouteService service;

    @BeforeEach
    void setUp() {
        lenient().when(camelContext.getRouteController()).thenReturn(routeController);
        lenient().when(routeController.getRouteStatus(anyString())).thenReturn(serviceStatus);
    }

    @Test
    void updateAllRoutes_exceptionsPassThrough() {
        String originalExceptionMessage = "unleash unavailable";
        when(unleash.isEnabled(anyString(), eq(false))).thenThrow(new RuntimeException(originalExceptionMessage));

        RuntimeException exception = assertThrowsExactly(RuntimeException.class, service::updateAllRoutes);

        assertEquals(originalExceptionMessage, exception.getMessage());
    }

    @Test
    void updateAllRoutes_stoppedRouteIsStarted() throws Exception {
        enableAllRoutes();
        when(serviceStatus.isStarted()).thenReturn(false);

        service.updateAllRoutes();

        verify(routeController).startRoute(POSTGRES_LOG_CONSUMER_ID);
    }

    @Test
    void updateAllRoutes_runningRouteIsStopped() throws Exception {
        for (FeatureFlagRouteService.RouteConfiguration routeConfig : FeatureFlagRouteService.RouteConfiguration.values()) {
            when(unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState())).thenReturn(false);
        }
        when(serviceStatus.isStarted()).thenReturn(true);

        service.updateAllRoutes();

        verify(routeController).stopRoute(POSTGRES_LOG_CONSUMER_ID);
    }

    @Test
    void updateAllRoutes_routeInCorrectStatusNotModified() throws Exception {
        enableAllRoutes();
        when(serviceStatus.isStarted()).thenReturn(true);

        service.updateAllRoutes();

        verify(routeController, never()).startRoute(anyString());
        verify(routeController, never()).stopRoute(anyString());
    }

    @Test
    void updateAllRoutes_allRoutesUpdated() throws Exception {
        enableAllRoutes();
        when(serviceStatus.isStarted()).thenReturn(false);

        service.updateAllRoutes();

        for (FeatureFlagRouteService.RouteConfiguration routeConfig : FeatureFlagRouteService.RouteConfiguration.values()) {
            verify(routeController).startRoute(routeConfig.getRouteId());
        }
    }

    private void enableAllRoutes() {
        for (FeatureFlagRouteService.RouteConfiguration routeConfig : FeatureFlagRouteService.RouteConfiguration.values()) {
            when(unleash.isEnabled(routeConfig.getFeatureFlag(), routeConfig.getDefaultState())).thenReturn(true);
        }
    }
}
