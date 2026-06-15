package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.GCPArkivLoggProducerProcessor;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class GCPArkivLoggProducer extends ArkivLoggProducer {

    @Override
    public void configure() {
        super.errorHandling();

        from(ARKIVLOGG_PRODUCER_ROUTE)
            .routeId(ARKIVLOGG_PRODUCER_ID)
            .log("Producing log message ${header.CamelFileName} to GCP Logging")
            .bean(GCPArkivLoggProducerProcessor.class, "incrementMetrics")
            .bean(GCPArkivLoggProducerProcessor.class, "writeToGcpLogging");
    }
}
