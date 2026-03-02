package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.boot.conditionals.Cluster;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoggkamelApp {

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        SpringApplication loggKamelApp = new SpringApplication(LoggkamelApp.class);
//        loggKamelApp.setAdditionalProfiles(ClusterUtils.getProfiles());
        loggKamelApp.setAdditionalProfiles(Cluster.profiler());
        loggKamelApp.run(args);
    }

}
