package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.InputStreamReader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.NativeLogPacketConsumerProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLocalOrTest
public class LocalNativeLogPacketConsumer extends NativeLogPacketConsumer {

    @Value("${routing.packet.bucket}")
    private String logPacketConsumerUri;

    public LocalNativeLogPacketConsumer(
            NativeLogPacketConsumerProcessor consumerProcessor,
            InputStreamReader inputStreamReader
    ) {
        super(consumerProcessor, inputStreamReader);
    }

    @Override
    public void configure() {
        configureConsumer(logPacketConsumerUri, consumerProcessor::populateLocalFilenameHeader);
    }
}
