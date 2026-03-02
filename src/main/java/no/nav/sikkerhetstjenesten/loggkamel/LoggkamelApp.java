package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.sikkerhetstjenesten.loggkamel.utils.ClusterUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoggkamelApp {

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        // TODO: remove once the customized profiler call is ready
//        SpringApplication.run(LoggkamelApp.class, args);

        SpringApplication loggKamelApp = new SpringApplication(LoggkamelApp.class);
        loggKamelApp.setAdditionalProfiles(ClusterUtils.getProfiles());
        loggKamelApp.run(args);
    }

}
