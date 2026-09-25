package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.GCP_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.GCP_STREAM;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.LOCAL_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogRouteConfiguration.LOCAL_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteConfigurationIdResolverTest {

    @Test
    void resolvesLocalConfigurations() {
        RouteConfigurationIdResolver resolver = resolverFor("local");

        assertEquals(LOCAL_STREAM, resolver.resolve(InputFileType.STREAM));
        assertEquals(LOCAL_PACKET, resolver.resolve(InputFileType.PACKET));
    }

    @Test
    void resolvesTestAsLocalConfigurations() {
        RouteConfigurationIdResolver resolver = resolverFor("test");

        assertEquals(LOCAL_STREAM, resolver.resolve(InputFileType.STREAM));
        assertEquals(LOCAL_PACKET, resolver.resolve(InputFileType.PACKET));
    }

    @Test
    void resolvesGcpConfigurations() {
        RouteConfigurationIdResolver devResolver = resolverFor("dev-gcp");
        RouteConfigurationIdResolver prodResolver = resolverFor("prod-gcp");

        assertEquals(GCP_STREAM, devResolver.resolve(InputFileType.STREAM));
        assertEquals(GCP_PACKET, devResolver.resolve(InputFileType.PACKET));
        assertEquals(GCP_STREAM, prodResolver.resolve(InputFileType.STREAM));
        assertEquals(GCP_PACKET, prodResolver.resolve(InputFileType.PACKET));
    }

    @Test
    void rejectsUnsupportedEnvironment() {
        RouteConfigurationIdResolver resolver = resolverFor("prod-fss");

        assertThrows(IllegalStateException.class, () -> resolver.resolve(InputFileType.STREAM));
    }

    private RouteConfigurationIdResolver resolverFor(String profile) {
        return new RouteConfigurationIdResolver(new MockEnvironment().withProperty("NAIS_CLUSTER_NAME", profile));
    }
}
