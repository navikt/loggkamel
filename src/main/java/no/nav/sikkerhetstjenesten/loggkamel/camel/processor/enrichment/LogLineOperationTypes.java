package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogLineOperationTypes {

    public static String LOG_LINE_OPERATION_TYPES = "LogLineOperationTypes";

    boolean isRead;
    boolean isModification;
}
