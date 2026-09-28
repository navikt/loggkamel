package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.GCP_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.GCP_STREAM;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.LOCAL_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.LOCAL_STREAM;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.ORIGINAL_FILE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ErrorRouteConfigurationTest {

    @TempDir
    Path tempDir;

    @Test
    void configuration_definesAllEnvironmentAndInputTypeConfigurations() throws Exception {
        ErrorRouteConfiguration configuration = new ErrorRouteConfiguration(mock(Metrics.class));

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

    @Test
    void convertMessageToLocalFileCopy_localBackoutUsesOriginalSourceFileAsBody() throws Exception {
        Path pathToSourceFile = Files.writeString(tempDir.resolve("source.log"), "original contents");
        DefaultExchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(ORIGINAL_FILE_PATH, pathToSourceFile.toString());

        new ErrorRouteConfiguration(mock(Metrics.class)).convertMessageToLocalFileCopy(exchange);

        assertEquals(pathToSourceFile.toFile(), exchange.getMessage().getBody(File.class));
    }

    @Test
    void convertMessageToLocalFileCopy_localBackoutFailsWhenOriginalSourceFileIsUnavailable() {
        DefaultExchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(ORIGINAL_FILE_PATH, tempDir.resolve("missing.log").toString());

        assertThrows(
                IllegalStateException.class,
                () -> new ErrorRouteConfiguration(mock(Metrics.class)).convertMessageToLocalFileCopy(exchange)
        );
    }
}
