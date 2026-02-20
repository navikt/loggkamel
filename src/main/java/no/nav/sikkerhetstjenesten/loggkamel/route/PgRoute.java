package no.nav.sikkerhetstjenesten.loggkamel.route;

import no.nav.sikkerhetstjenesten.loggkamel.bean.PgBean;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class PgRoute extends RouteBuilder {

    @Override
    public void configure() {
        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")
        //from("timer://myTimer?period=600000")
        from("file:src/main/resources/files?noop=true")
            .log("Message: ${body}, Headers: ${headers}")
            .split(simple("${body}").tokenize("^\\<|\n\\<"))
                //.setProperty("navIdent", body())    //simple("${body} regex '[A-Z]\\d{6}'"))
                //.log("Nav-ident: ${exchangeProperty.navIdent}")
                .log("Message: ${body}")
                .bean(PgBean.class, "extract")
                // Once the PgBean.extract() method has been run, confirm that you have added fields to the exchange variables that
                // can be read and logged
                .log("Per-message variables visible in the route after bean execution: ${variables}")
                .toD("file:src/main/resources/files/output/");
    }
}
