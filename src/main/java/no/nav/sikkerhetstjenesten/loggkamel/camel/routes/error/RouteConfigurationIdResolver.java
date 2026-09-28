package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import no.nav.boot.conditionals.Cluster;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.GCP_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.GCP_STREAM;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.LOCAL_PACKET;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.ErrorRouteConfiguration.LOCAL_STREAM;

@Component
public class RouteConfigurationIdResolver {

    private final Environment environment;

    public RouteConfigurationIdResolver(Environment environment) {
        this.environment = environment;
    }

    public String resolve(InputFileType inputFileType) {
        if (Cluster.LOCAL.isActive(environment) || Cluster.TEST.isActive(environment)) {
            return inputFileType == InputFileType.STREAM ? LOCAL_STREAM : LOCAL_PACKET;
        }
        if (Cluster.DEV_GCP.isActive(environment) || Cluster.PROD_GCP.isActive(environment)) {
            return inputFileType == InputFileType.STREAM ? GCP_STREAM : GCP_PACKET;
        }
        throw new IllegalStateException("No error handling route configuration for active environment");
    }
}
