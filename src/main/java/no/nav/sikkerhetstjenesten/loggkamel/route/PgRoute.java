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

        // TODO: configure and test reasonable retry pattern here
        errorHandler(deadLetterChannel("file:src/main/resources/files/output/dead-letter?fileExist=Append")
                .maximumRedeliveries(1)
                .useExponentialBackOff()
        );

        // TODO: define invalid message endpoint, for files in wrong encoding or that do not match expected log format

        // TODO: use injected Endpoints to define the in and out points for the messages
        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")
        //from("timer://myTimer?period=600000")
        from("file:src/main/resources/files?noop=true")
                .unmarshal().gzipDeflater()
//                .log("Message: ${body}, Headers: ${headers}")
                .split(simple("${body}").tokenize("^\\<|\n\\<"))
                .log("Message: ${body}, Headers: ${headers}")
                //.setProperty("navIdent", body())    //simple("${body} regex '[A-Z]\\d{6}'"))
                //.log("Nav-ident: ${exchangeProperty.navIdent}")
                .bean(PgBean.class, "extract")
                .log("Per-message variables visible in the route after bean execution: ${variables}")
                // TODO: Confirm that you can access individual variables by name here
                // TODO: Have the output location depend on (incorporate) db name
                .toD("file:src/main/resources/files/output/?fileExist=Append");
    }
}
