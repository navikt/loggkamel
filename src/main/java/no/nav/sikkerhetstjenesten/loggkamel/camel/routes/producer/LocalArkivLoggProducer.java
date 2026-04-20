package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
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
                .log("Producing log message ${header.CamelFileName} to local log")
                .process(exchange -> {
                    exchange.getMessage().setBody(objectMapper.writeValueAsString(exchange.getMessage().getBody(EnrichedAuditlogg.class)));
                })
                .toD(producerUri);
    }
}
