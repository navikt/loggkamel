package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.LocalStandardizedLogLineProducerProcessor;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class LocalStandardizedLogLineProducer extends StandardizedLogLineProducer {

    @Value("${routing.arkiv.producer}")
    String producerUri;

    @Override
    public void configure() {
        super.errorHandling();

        from(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE)
                .routeId(STANDARDIZED_LOG_LINE_PRODUCER_ID)
                .log(LoggingLevel.INFO, "Producing log message ${header.CamelFileName} to local log")
                .bean(LocalStandardizedLogLineProducerProcessor.class, "mapToJson")
                .bean(LocalStandardizedLogLineProducerProcessor.class, "prepareLogLineHeaders")
                .toD(producerUri);
    }
}
