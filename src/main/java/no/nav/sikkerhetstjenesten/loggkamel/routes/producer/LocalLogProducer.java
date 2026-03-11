package no.nav.sikkerhetstjenesten.loggkamel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class LocalLogProducer extends LogProducer {

    @Value("${routing.postgres.producer}")
    private String producerUri;

    @Override
    public void configure() {
        from(POSTGRES_LOG_PRODUCER_ROUTE)
                .routeId(POSTGRES_LOG_PRODUCER_ID)
                // TODO: enrich log message body with collected metadata
                .toD(producerUri);
    }
}
