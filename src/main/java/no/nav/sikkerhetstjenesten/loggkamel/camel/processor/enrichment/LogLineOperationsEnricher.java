package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogLineOperationsEnricher {

    private static final List<EnrichedAuditlogg.AuditClass> WRITE_CLASSES = List.of(EnrichedAuditlogg.AuditClass.WRITE, EnrichedAuditlogg.AuditClass.ROLE, EnrichedAuditlogg.AuditClass.DDL);

    public LogLineOperationTypes constructOperationTypesFromAuditClass(EnrichedAuditlogg.AuditClass auditClass) {
        LogLineOperationTypes routingAttributes = new LogLineOperationTypes();

        if (WRITE_CLASSES.contains(auditClass)) {
            routingAttributes.setModification(true);
        }
        if (EnrichedAuditlogg.AuditClass.READ.equals(auditClass)) {
            routingAttributes.setRead(true);
        }

        return routingAttributes;
    }
}
