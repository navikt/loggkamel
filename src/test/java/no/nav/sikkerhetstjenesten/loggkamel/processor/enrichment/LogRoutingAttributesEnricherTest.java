package no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LogRoutingAttributesEnricherTest {

    @Mock
    LogRoutingAttributes routingAttributes;

    @InjectMocks
    LogRoutingAttributesEnricher logRoutingAttributesEnricher;

    @Test
    void auditClassWrite_setsModificationTrue() {
        LogRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.WRITE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRole_setsModificationTrue() {
        LogRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.ROLE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassDdl_setsModificationTrue() {
        LogRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.DDL);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRead_setsReadTrue() {
        LogRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.READ);

        assertFalse(constructedAttributes.isModification());
        assertTrue(constructedAttributes.isRead());
    }

    @Test
    void auditClassOther_doesNotSetAnyFlags() {
        LogRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass("OTHER");

        assertFalse(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

}