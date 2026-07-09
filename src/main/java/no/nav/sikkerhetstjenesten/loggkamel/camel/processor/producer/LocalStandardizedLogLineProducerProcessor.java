package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.PLACE_IN_PACKET;
import static org.apache.camel.Exchange.FILE_NAME;

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

    public void prepareLogLineHeaders(Exchange exchange) {
        String logPacketFilename = exchange.getMessage().getHeader(FILE_NAME, String.class);
        Integer placeInPacket = exchange.getVariable(PLACE_IN_PACKET, Integer.class);

        if (logPacketFilename == null || logPacketFilename.isEmpty() || placeInPacket == null) {
            log.warn("Filename or PlaceInPacket placeInPacket header is missing while splitting log packet");
            throw new InvalidLogLineException("Filename or PlaceInPacket header is missing while splitting log packet");
        }

        String logLineFilename = addLogLineNumberToFilename(logPacketFilename,  placeInPacket);
        log.debug("New filename being assigned to packet: {}", logLineFilename);

        exchange.getMessage().setHeader(FILE_NAME, logLineFilename);
    }

    private String addLogLineNumberToFilename(String originalFileName, Integer placeInPacket) {
        String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return fileBeforeExtension + "." + placeInPacket + fileExtension;
    }
}

