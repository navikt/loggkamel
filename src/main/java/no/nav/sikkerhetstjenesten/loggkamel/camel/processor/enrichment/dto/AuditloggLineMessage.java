package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class AuditloggLineMessage {

    @NonNull
    String body;
    @NonNull
    AuditloggLineMessageHeader header;
}
