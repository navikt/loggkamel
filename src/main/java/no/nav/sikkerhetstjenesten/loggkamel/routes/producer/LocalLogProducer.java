package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.PostgresLogEnrichmentProcessor.LOG_ENRICHMENT;

@Component
@ConditionalOnLocalOrTest
public class LocalLogProducer extends LogProducer {

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                .log("Producing log message ${header.CamelFileName} to local log")
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getMessage().getBody() + ", enriched log info: " + exchange.getVariables().get(LOG_ENRICHMENT));
                })
                .toD(producerUri);
    }
}
