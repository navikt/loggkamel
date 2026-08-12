package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.boot.conditionals.Cluster;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class LoggkamelApp {
    public static void main(String[] args) {
        var loggKamelApp = new SpringApplication(LoggkamelApp.class);
        loggKamelApp.setAdditionalProfiles(Cluster.profiler());
        loggKamelApp.run(args);
    }
}
