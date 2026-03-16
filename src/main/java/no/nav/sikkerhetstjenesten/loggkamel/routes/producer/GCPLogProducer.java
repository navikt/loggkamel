package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnGCP;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnGCP
public class GCPLogProducer extends LogProducer {

    @Override
    public void configure() {
        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                // TODO: build new log body that is a json blob containing both original message body and collected metadata
                // TODO: instead of sending to a destination, send to a bean that uploads it to the google log api
                .toD(producerUri);
    }
}
