package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LogLineRoutingAttributesEnricherTest {

    @Mock
    LogLineRoutingAttributes routingAttributes;

    @InjectMocks
    LogRoutingAttributesEnricher logRoutingAttributesEnricher;

    @Test
    void auditClassWrite_setsModificationTrue() {
        LogLineRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.WRITE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRole_setsModificationTrue() {
        LogLineRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.ROLE);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassDdl_setsModificationTrue() {
        LogLineRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.DDL);

        assertTrue(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

    @Test
    void auditClassRead_setsReadTrue() {
        LogLineRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(LogRoutingAttributesEnricher.READ);

        assertFalse(constructedAttributes.isModification());
        assertTrue(constructedAttributes.isRead());
    }

    @Test
    void auditClassOther_doesNotSetAnyFlags() {
        LogLineRoutingAttributes constructedAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass("OTHER");

        assertFalse(constructedAttributes.isModification());
        assertFalse(constructedAttributes.isRead());
    }

}