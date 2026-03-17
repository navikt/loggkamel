package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class LocalLogProducer extends LogProducer {

    @Override
    public void configure() {
        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                //TODO: refine this from all message variables to just those we'd want to persist in log API
                .process(exchange -> {
                    exchange.getMessage().setBody(exchange.getMessage().getBody() + ", messageVariables: " + exchange.getVariables());
                })
                // INSERT GCP LOGS HERE FOR TESTING
                .toD(producerUri);
    }
}
