package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.GCP_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.GCP_STREAM;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.LOCAL_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.LOCAL_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LogRouteConfigurationTest {

    @Test
    void definesAllEnvironmentAndInputTypeConfigurations() throws Exception {
        LogRouteConfiguration configuration = new LogRouteConfiguration(mock(Metrics.class));
        configuration.setCamelContext(new DefaultCamelContext());
        ReflectionTestUtils.setField(configuration, "postgresLocalBackoutDirectoryUri", "direct:postgres-local-backout");
        ReflectionTestUtils.setField(configuration, "postgresBackoutBucketName", "postgres-backout-bucket");
        ReflectionTestUtils.setField(configuration, "postgresConsumerUri", "direct:postgres-consumer");
        ReflectionTestUtils.setField(configuration, "packetLocalBackoutDirectoryUri", "direct:packet-local-backout");
        ReflectionTestUtils.setField(configuration, "packetBackoutBucketName", "packet-backout-bucket");
        ReflectionTestUtils.setField(configuration, "packetConsumerUri", "direct:packet-consumer");

        configuration.configuration();

        Set<String> configurationIds = configuration.getRouteConfigurationCollection()
                .getRouteConfigurations()
                .stream()
                .map(routeConfiguration -> routeConfiguration.getId())
                .collect(Collectors.toSet());

        assertEquals(Set.of(LOCAL_STREAM, LOCAL_PACKET, GCP_STREAM, GCP_PACKET), configurationIds);
    }
}
