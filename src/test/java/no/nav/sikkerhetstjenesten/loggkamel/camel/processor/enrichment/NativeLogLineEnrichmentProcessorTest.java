package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogPacketException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeLogLineEnrichmentProcessorTest {

    private static final String NAV_IDENT = "navIdent";
    private static final String T_IDENT = "tIdent";
    private static final String EPOST = "epost@blah.bleh";

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Mock
    EnrichedAuditlogg  enrichedAuditlogg;

    @Mock
    ConstraintViolation<EnrichedAuditlogg> violation;

    @Mock
    Path path;

    @Mock
    Metrics metrics;

    @Mock
    EntraProxyService entraProxyService;

    @Mock
    Validator validator;

    NativeLogLineEnrichmentProcessor nativeLogLineEnrichmentProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        nativeLogLineEnrichmentProcessor = new NativeLogLineEnrichmentProcessor(metrics, entraProxyService, validator) {};
    }

    @Test
    void getAnsattEpostFromNavIdent_convertsEntraProxyException() {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(new RuntimeException());

        assertThrows(EntraProxyDependencyException.class, () -> nativeLogLineEnrichmentProcessor.getAnsattEpostFromNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattEpostFromNavIdent_emptyEmployeeIncrementsMetricsAndReturnsNull() {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn("");

        assertNull(nativeLogLineEnrichmentProcessor.getAnsattEpostFromNavIdent(NAV_IDENT));

        verify(metrics).incrementUnknownNavIdent();
    }

    @Test
    void getAnsattEpostFromNavIdent_happyPath() {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        assertEquals(EPOST, nativeLogLineEnrichmentProcessor.getAnsattEpostFromNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattEpostFromTIdent_convertsEntraProxyException() {
        when(entraProxyService.getAnsattFraTIdent(T_IDENT)).thenThrow(new RuntimeException());

        assertThrows(EntraProxyDependencyException.class, () -> nativeLogLineEnrichmentProcessor.getAnsattEpostFromTIdent(T_IDENT));
    }

    @Test
    void getAnsattEpostFromTIdent_emptyEmployeeIncrementsMetricsAndReturnsNull() {
        when(entraProxyService.getAnsattFraTIdent(T_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn("");

        assertNull(nativeLogLineEnrichmentProcessor.getAnsattEpostFromTIdent(T_IDENT));

        verify(metrics).incrementUnknownNavIdent();
    }

    @Test
    void getAnsattEpostFromTIdent_happyPath() {
        when(entraProxyService.getAnsattFraTIdent(T_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        assertEquals(EPOST, nativeLogLineEnrichmentProcessor.getAnsattEpostFromTIdent(T_IDENT));
    }

    @Test
    void validateEnrichedAuditlogg_validationExceptionsFound() {
        when(validator.validate(enrichedAuditlogg)).thenReturn(Set.of(violation));
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("message");

        assertThrows(InvalidLogPacketException.class, () -> nativeLogLineEnrichmentProcessor.validateEnrichedAuditlogg(enrichedAuditlogg));
    }

    @Test
    void validateEnrichedAuditlogg_noValidationExceptionsFound() {
        when(validator.validate(enrichedAuditlogg)).thenReturn(Collections.emptySet());

        nativeLogLineEnrichmentProcessor.validateEnrichedAuditlogg(enrichedAuditlogg);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "navIdent, NULL, true",
            "NULL, tIdent, true",
            "navIdent, tIdent, true",
            "NULL, NULL, false"
    }, nullValues = "NULL")
    void validateEnrichedAuditlogg_requiresAtLeastOneIdentity(String navIdent, String tIdent, boolean valid) {
        EnrichedAuditlogg auditlogg = EnrichedAuditlogg.builder()
                .logTime(ZonedDateTime.now())
                .navIdent(navIdent)
                .tIdent(tIdent)
                .dbName("database")
                .auditType(EnrichedAuditlogg.AuditType.SESSION)
                .pgAuditClass(EnrichedAuditlogg.AuditClass.READ)
                .sqlStatement("SELECT 1")
                .build();

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(auditlogg);

            assertEquals(valid, violations.isEmpty());
            if (!valid) {
                assertEquals(1, violations.size());
                assertEquals("navIdent or tIdent must be non-null", violations.iterator().next().getMessage());
            }
        }
    }

    @Test
    void serializeEnrichedAuditlogg_excludesIdentityValidationProperty() {
        EnrichedAuditlogg auditlogg = EnrichedAuditlogg.builder().tIdent(T_IDENT).build();

        var json = new ObjectMapper().valueToTree(auditlogg);

        assertEquals(T_IDENT, json.get("tIdent").asText());
        assertFalse(json.has("tident"));
        assertTrue(json.get("navIdent").isNull());
        assertFalse(json.has("identityPresent"));
    }

}