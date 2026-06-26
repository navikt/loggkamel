package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.LocalArkivLoggProducerProcessor;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class LocalArkivLoggProducer extends ArkivLoggProducer {

    @Value("${routing.arkiv.producer}")
    String producerUri;

    @Override
    public void configure() {
        super.errorHandling();

        from(ARKIVLOGG_PRODUCER_ROUTE)
                .routeId(ARKIVLOGG_PRODUCER_ID)
                .log(LoggingLevel.INFO, "Producing log message ${header.CamelFileName} to local log")
                .bean(LocalArkivLoggProducerProcessor.class, "mapToJson")
                .toD(producerUri);
    }
}
