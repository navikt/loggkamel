package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LogLineOperationTypesEnricherTest {

    @Mock
    LogLineOperationTypes routingAttributes;

    @InjectMocks
    LogLineOperationsEnricher logLineOperationsEnricher;

    @Test
    void auditClassWrite_setsModificationTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(LogLineOperationsEnricher.WRITE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRole_setsModificationTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(LogLineOperationsEnricher.ROLE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassDdl_setsModificationTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(LogLineOperationsEnricher.DDL);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRead_setsReadTrue() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(LogLineOperationsEnricher.READ);

        assertFalse(constructedAttributes.isModification());
        assertTrue(constructedAttributes.isRead());
    }

    @Test
    void auditClassOther_doesNotSetAnyFlags() {
        LogLineOperationTypes constructedAttributes = logLineOperationsEnricher.constructOperationTypesFromAuditClass("OTHER");

        assertFalse(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

}