package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.maintenance;

import io.getunleash.Unleash;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.spi.RouteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerControlRouteProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    CamelContext  camelContext;

    @Mock
    RouteController  routeController;

    @Mock
    Unleash unleash;

    @InjectMocks
    ConsumerControlRouteProcessor processor;

    @BeforeEach
    void setUp() {
        when(exchange.getContext()).thenReturn(camelContext);
        when(camelContext.getRouteController()).thenReturn(routeController);
    }

}