package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor.LOG_ENRICHMENT;

@Component
@ConditionalOnLocalOrTest
public class LocalLogProducer extends LogProducer {

    @Value("${routing.postgres.producer}")
    String producerUri;

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                .log("Producing log message ${header.CamelFileName} to local log")
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getVariables().get(LOG_ENRICHMENT).toString());
                })
                .toD(producerUri);
    }
}
