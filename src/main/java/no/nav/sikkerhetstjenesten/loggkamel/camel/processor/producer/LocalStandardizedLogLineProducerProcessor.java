package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogGroupException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.NativeLogStreamSplitterProcessor;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Service
public class LocalStandardizedLogLineProducerProcessor {

    private static final Logger log = LoggerFactory.getLogger(LocalStandardizedLogLineProducerProcessor.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public LocalStandardizedLogLineProducerProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void mapToJson(Exchange exchange) throws JsonProcessingException {
        EnrichedAuditlogg enrichedAuditlogg = exchange.getMessage().getBody(EnrichedAuditlogg.class);
        exchange.getMessage().setBody(objectMapper.writeValueAsString(enrichedAuditlogg));
    }

    //TODO: unit tests
    public void prepareLogLineHeaders(Exchange exchange) {
        String logPacketFilename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (logPacketFilename == null || logPacketFilename.isEmpty()) {
            log.warn("Filename header is missing while splitting log packet");
            throw new InvalidLogGroupException("Filename header is missing while splitting log packet");
        }

        String logLineFilename = createFilenameWithUUID(logPacketFilename);
        log.debug("New filename being assigned to packet: {}", logLineFilename);

        exchange.getMessage().setHeader(FILE_NAME, logLineFilename);
        exchange.getMessage().setHeader(OBJECT_NAME, logLineFilename);
    }

    private String createFilenameWithUUID(String originalFileName) {
        String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return fileBeforeExtension + "." + UUID.randomUUID() + fileExtension;
    }
}

