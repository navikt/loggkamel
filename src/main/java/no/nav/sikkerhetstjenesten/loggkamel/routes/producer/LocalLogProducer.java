package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.processor.PostgresLogEnrichmentProcessor.LOG_VALUES;

@Component
@ConditionalOnLocalOrTest
public class LocalLogProducer extends LogProducer {

    @Override
    public void configure() {
        super.errorHandling();

        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getMessage().getBody() + ", enriched log info: " + exchange.getVariables().get(LOG_VALUES));
                })
                .toD(producerUri);
    }
}
