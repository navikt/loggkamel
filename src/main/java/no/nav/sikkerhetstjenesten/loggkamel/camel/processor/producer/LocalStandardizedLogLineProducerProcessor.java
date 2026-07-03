package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.EnrichedAuditlogg;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalStandardizedLogLineProducerProcessor {

    private final ObjectMapper objectMapper;

    @Autowired
    public LocalStandardizedLogLineProducerProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void mapToJson(Exchange exchange) throws JsonProcessingException {
        EnrichedAuditlogg enrichedAuditlogg = exchange.getMessage().getBody(EnrichedAuditlogg.class);
        exchange.getMessage().setBody(objectMapper.writeValueAsString(enrichedAuditlogg));
    }
}

