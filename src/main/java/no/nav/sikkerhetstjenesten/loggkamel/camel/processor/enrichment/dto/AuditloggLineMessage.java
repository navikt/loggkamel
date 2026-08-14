package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
public class AuditloggLineMessage {

    @NonNull
    String body;
    @NonNull
    AuditloggLineMessageHeader header;
}
