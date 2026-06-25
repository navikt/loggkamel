package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLineOperationTypesEnricherTest {

    LogLineOperationsEnricher logLineOperationsEnricher = new  LogLineOperationsEnricher();

    @Test
    void auditClassWrite_setsModificationTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(EnrichedAuditlogg.AuditClass.WRITE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRole_setsModificationTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(EnrichedAuditlogg.AuditClass.ROLE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassDdl_setsModificationTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(EnrichedAuditlogg.AuditClass.DDL);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRead_setsReadTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(EnrichedAuditlogg.AuditClass.READ);

        assertFalse(constructedAttributes.isModification());
        assertTrue(constructedAttributes.isRead());
    }

    @Test
    void auditClassOther_doesNotSetAnyFlags() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(EnrichedAuditlogg.AuditClass.MISC);

        assertFalse(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

}