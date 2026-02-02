package no.nav.sikkerhetstjenesten.loggkamel.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class MySpringBootRouter extends RouteBuilder {

    @Override
    public void configure() {
        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")
        from("timer://myTimer?period=600000")
            .log("hello");
    }
}
