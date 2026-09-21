package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogStreamConsumerProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class LocalPostgresLogStreamConsumer extends PostgresLogStreamConsumer {

    @Value("${routing.postgres.consumer}")
    private String postgresStreamConsumerUri;

    public LocalPostgresLogStreamConsumer(
            PostgresLogStreamConsumerProcessor consumerProcessor,
            InputStreamReader inputStreamReader
    ) {
        super(consumerProcessor, inputStreamReader);
    }

    @Override
    public void configure() {
        configureConsumer(postgresStreamConsumerUri, consumerProcessor::initializeLocalConsumerState);
    }
}
