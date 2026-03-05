package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.boot.conditionals.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static no.nav.boot.conditionals.Cluster.NAIS_CLUSTER_NAME;

@SpringBootApplication
public class LoggkamelApp {
    private static final Logger log = LoggerFactory.getLogger(LoggkamelApp.class);

    public static void main(String[] args) {
        var loggKamelApp = new SpringApplication(LoggkamelApp.class);
        loggKamelApp.setAdditionalProfiles(Cluster.profiler());
        //TODO: remove after troubleshooting
        log.error("System environment is: " + System.getenv(NAIS_CLUSTER_NAME));
        loggKamelApp.run(args);
    }
}
