package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogLineOperationsEnricher {
    public static final String WRITE = "WRITE";
    public static final String ROLE = "ROLE";
    public static final String DDL = "DDL";
    public static final String READ = "READ";

    private static final List<String> WRITE_CLASSES = List.of(WRITE, ROLE, DDL);


    public LogLineOperationTypes constructOperationTypesFromAuditClass(String auditClass) {
        LogLineOperationTypes routingAttributes = new LogLineOperationTypes();

        if (WRITE_CLASSES.contains(auditClass)) {
            routingAttributes.setModification(true);
        }
        if (READ.equals(auditClass)) {
            routingAttributes.setRead(true);
        }

        return routingAttributes;
    }
}
